import copy
import json
from collections import namedtuple as nt
from os.path import join
from typing import List, Optional, Callable

from comby import Comby, Match, BoundTerm

from PrettyPrint import pretty
from RW import readAll
from TypeChangeAnalysis_pb2 import TypeChangeAnalysis
import javalang
import re

# "Before": ":[6~\b[A-Z]*\\w*].:[2](:[1])",
# "After": ":[5~\b[A-Z]*\\w*].:[4](:[1])"
##

with open("AdaptUsagePatterns.json") as f:
    AdaptationUsagePatterns = json.load(f)

with open("AdaptInitializers.json") as f:
    AdaptInitializers = json.load(f)

comby = Comby()
projects = readAll("Projects", "Project", protos=join('/Users/ameya/Research/TypeChangeStudy',
                                                      'TypeChangeMiner/Input/ProtosOut/'))

TypeChange = nt('TypeChange', ['source_type', 'target_type'])


def pretty_type_change(tc):
    return tc.source_type + "->" + tc.target_type


Instance = nt("Instance", ["nameBefore", "nameAfter", "statementBefore", "statementAfter"])

Rule = nt("Rule", ["Name", "Match", "Replace", "MappingBefore", "MappingAfter"])

Template = nt("Template", ["Name", "Match", "Replace", "mappings_before", "mappings_after"])

typeChange_projects = {}
typeChange_commits = {}
typeChange_instances = {}


def get_type_changes():
    global typeChange, items_
    for project in projects:
        type_change_commits = readAll("TypeChangeCommit_" + project.name, "TypeChangeCommit",
                                      protos='/Users/ameya/Research/TypeChangeStudy/TypeChangeMiner/Output/')
        for cmt in type_change_commits:
            tc: TypeChangeAnalysis
            for tc in cmt.typeChanges:
                typeChange = TypeChange(pretty(tc.b4), pretty(tc.aftr))
                typeChange_projects.setdefault(typeChange, set()).add(project.name)
                typeChange_commits.setdefault(typeChange, set()).add(cmt.sha)
                typeChange_instances.setdefault(typeChange, []).extend(list(tc.typeChangeInstances))
    unpopular_type_changes = [k for k, v in typeChange_projects.items() if len(v) == 1]
    for t in unpopular_type_changes:
        del typeChange_projects[t]
        del typeChange_commits[t]
        del typeChange_instances[t]
    items_ = [(k, v) for k, v in typeChange_instances.items() if "java.io.File" in k and "java.nio.file.Path" in k]
    items_ = sorted(items_, key=lambda x: len(x[1]), reverse=True)
    return items_


def match(source, template):
    cm: List[Match] = list(comby.matches(source=source, template=template, language=".java"))
    return len(cm) > 0


def get_template_variable_for(identifier):
    return ':[1~\\b' + identifier + "\\b]"


def try_comby_match(source, template):
    try:
        return list(comby.matches(source=source, template=template, language=".java"))
    except:
        return []


def instantiate_adapt_usage_meta_patterns(name_before: str, name_after: str) -> dict:
    patterns = copy.deepcopy(AdaptationUsagePatterns)
    for ptrn in patterns['Patterns']:
        for mpg in ptrn['Mappings']:
            mpg['Before'] = mpg['Before'].replace(":[1]", get_template_variable_for(name_before))
            mpg['After'] = mpg['After'].replace(":[1]", get_template_variable_for(name_after))
    return patterns


def abstract_adapt_usage_meta_patterns(name_before: str, name_after: str, patterns: List[Template], code_mapping) -> \
        List[Template]:
    def fn(s, name):
        return s.replace(get_template_variable_for(name), ":[1]")

    return [Template(p.Name, fn(p.Match, name_before), fn(p.Replace, name_after), p.mappings_before, p.mappings_after)
            for p in patterns]


def get_matching_templates_reference(statement_mapping, name_b4, name_after) -> List[Template]:
    adapt_references = instantiate_adapt_usage_meta_patterns(name_b4, name_after)
    matched_templates = []
    for pattern in adapt_references['Patterns']:
        for mapping in pattern['Mappings']:
            before_template = mapping['Before']
            after_template = mapping['After']
            ss_before_template = try_comby_match(source=statement_mapping.b4, template=before_template)
            ss_after_template = try_comby_match(source=statement_mapping.b4, template=after_template)
            if after_template not in before_template and bool(ss_before_template) and not bool(ss_after_template) \
                    or (after_template in before_template and bool(ss_before_template) and bool(ss_after_template)):
                tt_before_template = try_comby_match(source=statement_mapping.after, template=before_template)
                tt_after_template = try_comby_match(source=statement_mapping.after, template=after_template)

                if (before_template not in after_template and bool(tt_after_template) and not bool(tt_before_template)) \
                        or (before_template in after_template and bool(tt_after_template) and bool(tt_before_template)):
                    template = Template(Name=pattern['Pattern'], Match=before_template, Replace=after_template,
                                        mappings_before={k: v11 for k, v11 in
                                                         ss_before_template[0].environment.items()},
                                        mappings_after={k: v22 for k, v22 in tt_after_template[0].environment.items()})
                    matched_templates.append(template)
    return matched_templates


# any(match_to_meta_pattern(v, variable_mapping_target[k], renames))
def valid_mappings(variable_mapping_src: dict, variable_mapping_target: dict, renames: dict) -> bool:
    # same keys have same values

    s = all(variable_mapping_target[k] == variable_mapping_src[k] or any(
        variable_mapping_target[k].replace(b, a) == variable_mapping_src[k] for b, a in renames.items())
            for k in set(variable_mapping_src.keys()).intersection(set(variable_mapping_target.keys())))

    # different keys do not have same values
    dd = {k: variable_mapping_src[k] if k in variable_mapping_src.keys() else variable_mapping_target[k]
          for k in set(variable_mapping_src.keys()).symmetric_difference(variable_mapping_target.keys())}
    d = len(set(dd.values())) == len(dd)
    return s  # and d


def is_valid_java_expression(source):
    try:
        javalang.parse.parse_expression(source)
        return True
    except:
        return False


def get_longest_valid_subexpr(source):
    tokens = list(javalang.parse.tokenize(source, ignore_errors=True))
    longest_valid_expr = []
    for i in range(0, len(tokens) + 1):
        for j in range(i, len(tokens) + 1):
            sub_expr = tokens[i:j]
            if len(sub_expr) <= len(longest_valid_expr):
                continue
            if is_valid_java_expression(javalang.tokenizer.reformat_tokens(sub_expr).replace('\n', '')) and len(
                    sub_expr) > len(longest_valid_expr):
                longest_valid_expr = sub_expr
    return javalang.parse.parse_expression(javalang.tokenizer.reformat_tokens(longest_valid_expr).replace('\n', ''))


sss = get_longest_valid_subexpr("int f = fn.apply(4);")
print(sss)
print()


#
#
# def get_matching_templates_initializer(statement_mapping, renames={}) -> dict:
#     src = statement_mapping.b4
#     trgt = statement_mapping.after
#     matched_templates = {}
#
#     for pattern in AdaptInitializers['Patterns']:
#
#         for mapping in pattern['Mappings']:
#             rule: Rule = Rule(Name=pattern['Pattern'] + ":" + mapping['Name'], Match=mapping['Before'],
#                               Replace=mapping['After'])
#
#             sub_expr_matches_before = {}
#             for sub_expr in get_all_valid_sub_expressions(src):
#                 matches = try_comby_match(source=sub_expr, template=rule.Match)
#                 if len(matches) > 1:
#                     sub_expr_matches_before[sub_expr] = matches[0].environment
#
#             sub_expr_matches_after = {}
#             for sub_expr in get_all_valid_sub_expressions(trgt):
#                 matches = try_comby_match(source=sub_expr, template=rule.Replace)
#                 if len(matches) >= 1:
#                     sub_expr_matches_after[sub_expr] = matches[0].environment
#
#             def valid(tv1: str, tv2: str, bt1: BoundTerm, bt2: BoundTerm) -> bool:
#                 if tv1 == tv2:
#                     return bt1.fragment == bt2.fragment
#                 elif ('m' + tv1) == tv2:
#                     return True
#                 else:
#                     return bt1.fragment != bt2.fragment
#
#             valid_sub_expr_mapping = []
#
#             for sub_expr_before, env1 in sub_expr_matches_before.items():
#                 for sub_expr_after, env2 in sub_expr_matches_after.items():
#                     if sub_expr_after == sub_expr_before:
#                         continue
#                     if all(valid(tv_a, tv_b, bt_b, bt_a) for tv_b, bt_b in env1.items() for tv_a, bt_a in env2.items()):
#                         valid_sub_expr_mapping.append((sub_expr_before, sub_expr_after))
#             if bool(valid_sub_expr_mapping):
#                 matched_templates[rule] = valid_sub_expr_mapping
#
#             #
#             #
#             # tt = try_comby_match(source=trgt, template=after_template)
#             # if bool(ss) and bool(tt):
#             #     variable_mapping_src = {k: v.fragment for k, v in ss[0].environment.items()}
#             #     variable_mapping_target = {k: v.fragment for k, v in tt[0].environment.items()}
#             #     if valid_mappings(variable_mapping_src, variable_mapping_target, renames):
#             #         matched_templates.append(
#             #             Template(Name=pattern['Pattern'], Match=before_template, Replace=after_template,
#             #                      mappings_before={k: v11 for k, v11 in ss[0].environment.items()},
#             #                      mappings_after={k: v22 for k, v22 in tt[0].environment.items()}))
#
#     return matched_templates


def is_not_just_rename_string_update(replacements):
    return any(r.replacementType not in ["VARIABLE_NAME", "STRING_LITERAL"] for r in replacements)


def get_regex_associated_to_tv(template) -> dict:
    def get_digit_in(s) -> str:
        for i in range(0, 10):
            if str(i) in s:
                return str(i)

    mapping = {}
    matches = re.findall(r":\[.*?]", template)
    for m in matches:
        if "~" in m:
            mapping[get_digit_in(m)] = m
    return mapping


# get_regex_associated_to_tv(":[1~\\w].:[2](:[3])")


def infer():
    inferred_rules = {}

    popular_type_changes = get_type_changes()

    for type_change in popular_type_changes:
        print(type_change[0])
        worth_learning = {}
        i = 100
        for instance in type_change[1]:
            if i == 0:
                continue
            i -= 1
            for c in instance.codeMapping:
                if not c.isSame and is_not_just_rename_string_update(c.replcementInferred):
                    rule: Optional[Rule] = infer_rule_for_statements(c, instance)
                    if rule:
                        worth_learning.setdefault((rule.Name, rule.Match, rule.Replace), []).append(
                            (rule.MappingBefore, rule.MappingAfter))

        rules = []
        for rule, mappings in worth_learning.items():
            updated_rule = {"Name": rule[0], "Match": rule[1], "Replace": rule[2]}
            before_mappings = {}
            for m in mappings:
                for k, v in m[0].items():
                    before_mappings.setdefault(k, set()).add(v)

            for tv, fragments in before_mappings.items():
                if len(fragments) == 1 and tv != str(1):
                    mp = get_regex_associated_to_tv(updated_rule['Match'])
                    x = mp[tv].replace(":[", "").replace("]", "") if tv in mp else tv
                    updated_rule['Match'] = comby.substitute(updated_rule['Match'], {x: list(fragments)[0]})

            after_mapping = {}
            for m in mappings:
                for k, v in m[1].items():
                    after_mapping.setdefault(k, set()).add(v)

            for tv, fragments in after_mapping.items():
                if len(fragments) == 1 and tv != str(1):  # and fragments[0].startswith:
                    updated_rule['Replace'] = comby.substitute(updated_rule['Replace'], {tv: list(fragments)[0]})
                    print()

            rules.append(updated_rule)

        if bool(worth_learning.keys()):
            inferred_rules[pretty_type_change(type_change[0])] = rules

    with open("InferredRules.json", 'w+') as fp:
        json.dump(inferred_rules, fp)

    #
    # if instance.elementKindAffected == 4:
    #
    # elif not try_comby_match(source=code_mapping.b4,
    #                          template=get_template_variable_for(instance.nameB4) + ":[]=:[2]"):
    #
    # else:
    #     matching_templates = get_matching_templates_initializer(code_mapping)


def check_pattern(before_template, after_template, code_mapping, name_before, name_after):
    tv1 = get_template_variable_for(name_before)

    op = comby.rewrite(code_mapping.b4, before_template.replace(":[1]", tv1),
                       after_template.replace(":[1]", name_after), language=".java")
    return op.replace("\n", "") == code_mapping.after.replace("\n", "")


def infer_rule_for_statements(code_mapping, instance) -> Optional[Rule]:
    print(instance.nameB4, instance.nameAfter)
    print(code_mapping.b4, code_mapping.after)
    print("---")
    matching_templates = abstract_adapt_usage_meta_patterns(instance.nameB4, instance.nameAfter,
                                                            get_matching_templates_reference(code_mapping,
                                                                                             instance.nameB4,
                                                                                             instance.nameAfter),
                                                            code_mapping)

    # is not a usage
    if not bool(matching_templates):
        print("Is not an usage!")
        # print(instance.nameB4, instance.nameAfter)
        # matching_templates = get_matching_templates_initializer(code_mapping)
        print()
    # is a usage
    else:
        for matching_template in matching_templates:
            variants = {k for k in matching_template.mappings_before.keys()}.symmetric_difference(
                {k for k in matching_template.mappings_after.keys()})
            before_variant_mapping = {i: matching_template.mappings_before[i].fragment for i in variants if
                                      i in matching_template.mappings_before.keys()}
            after_variant_mapping = {i: matching_template.mappings_after[i].fragment for i in variants if
                                     i in matching_template.mappings_after.keys()}

            rename_template_variable = {}

            # for k1, v1 in before_variant_mapping.items():
            #     for k2, v2 in after_variant_mapping.items():
            #         if v1 == v2:
            #             rename_template_variable[k2] = k1
            #
            # for k, v in rename_template_variable.items():
            #     after_variant_mapping[k] = ":[" + v + "]"
            #     before_variant_mapping[v] = ":[" + v + "]"

            before_template = comby.substitute(template=matching_template.Match,
                                               args=before_variant_mapping)
            after_template = comby.substitute(template=matching_template.Replace,
                                              args=after_variant_mapping)

            # output = comby.rewrite(code_mapping.b4, before_template, after_template, language=".java")
            # if check_pattern(before_template, after_template, code_mapping, instance.nameB4, instance.nameB4):
            rule = Rule(Name=matching_template[0] + ":" + matching_template.Name, Match=before_template,
                        Replace=after_template,
                        MappingBefore={k: v.fragment for k, v in matching_template.mappings_before.items()},
                        MappingAfter={k: v.fragment for k, v in matching_template.mappings_after.items()})
            print(instance.nameB4, instance.nameAfter)
            print(rule)
            return rule

    # if bool(matching_templates):
    #     for matching_template in matching_templates:
    #
    #         variants = {k for k in matching_template.mappings_before.keys()}.symmetric_difference(
    #             {k for k in matching_template.mappings_after.keys()})
    #         before_variant_mapping = {i: matching_template.mappings_before[i] for i in variants if
    #                                   i in matching_template.mappings_before.keys()}
    #         after_variant_mapping = {i: matching_template.mappings_after[i] for i in variants if
    #                                  i in matching_template.mappings_after.keys()}
    #
    #         rename_template_variable = {}
    #
    #         for k1, v1 in before_variant_mapping.items():
    #             for k2, v2 in after_variant_mapping.items():
    #                 if v1 == v2:
    #                     rename_template_variable[k2] = k1
    #
    #         for k, v in rename_template_variable.items():
    #             after_variant_mapping[k] = ":[" + v + "]"
    #             before_variant_mapping[v] = ":[" + v + "]"
    #
    #         before_template = comby.substitute(template=matching_template.Match,
    #                                            args=before_variant_mapping)
    #         after_template = comby.substitute(template=matching_template.Replace,
    #                                           args=after_variant_mapping)
    #
    #         return Rule(Name=matching_template[0] + ":" + matching_template.Name,
    #                     Match=before_template, Replace=after_template)
    # else:
    #     print("No match found for: ")
    #     print(code_mapping.b4)
    #     print(code_mapping.after)
    #     get_matching_templates_reference(code_mapping, instance.nameB4, instance.nameAfter)
    #     print()

    return None


infer()
print()

'''




For each statement, break it into updates
    Each update can either be performed using the input templates or just as a concrete replacement
    Each update is actually a tree of update. First update the subtree, then update the root
    


def fn1(statements, names):
    src = statements[0]
    trgt = statements[1]
    if isUsage(sm):
        
        src_exprs = getAllSubExpr(src)
        trgt_exprs = getAllSubExpr(src)
        
        template_variable_mappings = getMatched(sm, templates)
        
        # Get the smallest expression containing string covering all the template expressions
        
        cleaner_template_variable_mappings, expressions = getExpressionsFor(sm, template_variable_mappings)
        
    if assignment(sm):
        exps = take_lhs(statements)
        
        template_variable_mappings = getMatched(exps, templates)
        
        # Get the smallest expression containing string covering all the template expressions
        
        cleaner_template_variable_mappings, expressions = getExpressionsFor(sm, template_variable_mappings)
    
    return  instantiate(cleaner_template_variable_mappings, expressions)
        
        
        
        
'''
