from collections import namedtuple
from functools import partial
from typing import Iterator, List

from comby import Comby, Match
import javalang
tree = javalang.parse.tokenize("if(x>5){", ignore_errors=True)
tree1 = javalang.parse.parse_expression("rawHash&=Integer.MAX_VALUE")
print()
import json

comby = Comby()

Mappings = {("java.util.function.Function<java.lang.Integer,java.lang.Integer>",
             "java.util.function.IntUnaryOperator"): [lambda x: rename_call_site('apply', 'applyAsInt', x)],
            ("int", "long"): [lambda x: to_long_literal(x), lambda x: integer_to_long_parse(x)]}


def adapt(before_after, old_source):
    transformations = Mappings[before_after]
    for tx in transformations:
        new_source = tx(old_source)
        if new_source != old_source:
            return new_source
    return "No Transformation Found"


def rename_call_site(before_name, after_name, source):
    return comby.rewrite(source, ":[1]." + before_name + "(:[2])", ":[1]." + after_name + "(:[2])",
                         language=".java")


def to_long_literal(source):
    return comby.rewrite(source, r":[1~\d+]", ":[1]l")


def integer_to_long_parse(source):
    return comby.rewrite(source, "Integer.parseInt(:[1])", "Long.parseLong(:[1])")


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
    return s #and d


def try_comby_match(source, template):
    try:
        return list(comby.matches(source=source, template=template, language=".java"))
    except:
        return []


def match_to_meta_pattern(source: str, target: str, renames) -> []:
    if source == target:
        return []
    result = []
    with open("MetaPatterns.json") as f:
        patterns = json.load(f)
        for pattern in patterns['Patterns']:
            for mapping in pattern['Mappings']:
                before_template = mapping['Before']
                after_template = mapping['After']
                ss = try_comby_match(source=source, template=before_template)
                tt = try_comby_match(source=target, template=after_template)
                if len(ss) == 1 and len(tt) == 1:
                    variable_mapping_src = {k: v.fragment for k, v in ss[0].environment.items()}
                    variable_mapping_target = {k: v.fragment for k, v in tt[0].environment.items()}
                    if valid_mappings(variable_mapping_src, variable_mapping_target, renames):
                        print("Matched to " + pattern['Pattern'] + ":" + mapping['Name'])
                        variants = {k for k in variable_mapping_src.keys()}.symmetric_difference(
                            {k for k in variable_mapping_target.keys()})
                        before_variant_mapping = {i: variable_mapping_src[i] for i in variants if
                                                  i in variable_mapping_src.keys()}
                        after_variant_mapping = {i: variable_mapping_target[i] for i in variants if
                                                 i in variable_mapping_target.keys()}

                        before_template = comby.substitute(template=before_template, args=before_variant_mapping)
                        after_template = comby.substitute(template=after_template, args=after_variant_mapping)
                        result.append({"Before": before_template, "After": after_template})
    if len(result) > 0:
        for res in result:
            source = comby.rewrite(source, res['Before'], res['After'], language=".java")
        result.extend(match_to_meta_pattern(source, target, renames))
    return result

# #
print("-------")
for r in match_to_meta_pattern("Integer.valueOf(6)", "Long.valueOf(6)", {}):
    print(r)
