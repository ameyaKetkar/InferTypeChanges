import itertools
import json
import os
from os.path import dirname as parent
from pydoc import html

from jinja2 import Environment, FileSystemLoader
from json2html import json2html
from collections import namedtuple


# fileDir = parent((os.path.realpath('__file__')))
# print(fileDir)

Mapping = namedtuple('Mapping', ['Match', 'Replace'])
Instance = namedtuple('Instance', ['OriginalCompleteBefore', 'OriginalCompleteAfter', 'Before', 'After', 'Project'
    , 'Commit', 'CompilationUnit'])

fileDir = parent('/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/')
env = Environment(loader=FileSystemLoader(os.path.join(fileDir, "templates")))

GeneralDicts = lambda data: GeneralDictsTemplate.render(cols=data[0], body=data[1], Title=data[2], col_types=data[3],
                                                        css=data[4])

GeneralDictsTemplate = env.get_template("GeneralDictionariesTemplate.html")


def apply_template_to(write_to_file, data):
    with open(os.path.join(write_to_file), 'w+') as fh:
        fh.write(GeneralDicts(data))
        fh.close()


def json_to_html(json_data):
    return json2html.convert(json=json_data, escape=True)


def createHTMLTableFor(typeChange, mappingSummary, forWhat, htmlPage):
    if forWhat == 'Statement Mappings':
        colNames = ['Match', 'Replace', 'Instances', 'isVeryGood', 'isGood','isChange', 'Relevant Instances', 'Commits', 'Project',
                    'Link']
        col_types = '[\'string\',\'string\', \'number\', \'boolean\', \'boolean\', \'boolean\', \'number\', \'number\',\'number\',\'string\']'
        Title = 'Mapping Summary for ' + str(typeChange)
        body = []
        for k, v1 in mappingSummary.items():
            body.append([str(v1[c]) for c in colNames])

        apply_template_to(htmlPage, (colNames, body, Title, col_types, "../.."))
    if forWhat == 'Type Change Summary':
        colNames = ['Before Type', 'After Type', 'Very Good Mappings', 'Good Mappings','No. of Commits','No. of Projects', 'Mappings']
        col_types = '[\'string\',\'string\',\'number\', \'number\',\'number\', \'number\', \'number\']'
        Title = 'TypeChange Summary'
        body = []
        for k, v1 in mappingSummary.items():
            body.append([str(v1[c]) for c in colNames])
        apply_template_to(htmlPage, (colNames, body, Title, col_types, ".."))


excludeTcs = [('java.lang.String', 'java.util.Optional<:[tar]>')]

res = "/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/ResultFinalExperiment"
if not os.path.exists(res):
    os.mkdir(res)


def cleanup(k):
    return k[0] != k[1] and \
           k[0] not in ['var', 'val', 'java.lang.Void', 'java.lang.Object', 'void'] \
           and k[1] not in ['var', 'val', 'java.lang.Void', 'java.lang.Object', 'void'] \
           and len(k[0]) > 1 and len(k[1]) > 1 and k[0] != k[1] \
           and not k[1].endswith(k[0]) and not k[0].endswith(k[1]) \
           and '?' not in k[0] and '?' not in k[1] \
           and not k[1].startswith(k[0] + "<") and not k[0].startswith(k[1] + "<") \
           and not any(x in k[0] or x in k[1] for x in
                       ['java.lang.Object, java.lang.Number', 'java.lang.Exception', 'java.lang.RuntimeException',
                        'java.lang.Throwable', 'IOException', 'FileNotFoundException'])
type_change_ids = {}
with open(os.path.join(parent(res), "finalExperimentOpUpdated.jsonl")) as c:
    # with open(os.path.join(parent(res), "finalExperimentOp2.jsonl")) as c1:
        lines = c.readlines()
        # lines.extend(c1.readlines())
        mappings = [json.loads(l) for l in lines if l != '\n']
        tcTemplate_mapping = {}

        for m in mappings:
            tcTemplate_mapping.setdefault((m['BeforeTypeTemplate'], m['AfterTypeTemplate']),
                                          {}).setdefault((m['Match'], m['Replace']), []).append(m['Instance'])

        typeChangeID = 0
        typeChangeSummary = {}
        for typeChange, mappings in tcTemplate_mapping.items():
            if typeChange in excludeTcs or not cleanup(typeChange):
                continue

            i = 0
            safe_mappings = 0
            type_change_ids[typeChangeID] = typeChange
            mappingSummary = {}
            typeChangeFolder = os.path.join(res, "TypeChange{0}".format(str(typeChangeID)))
            if not os.path.exists(typeChangeFolder):
                os.mkdir(typeChangeFolder)
            veryGoodMappingCounter = 0
            goodMappingCounter = 0
            # commits = set()
            mapping_id_tracker = {}
            all_commits = {inst['Commit'] for matchReplace, instances in mappings.items() for inst in instances}
            all_projects = {inst['Project'] for matchReplace, instances in mappings.items() for inst in instances}
            u = 0
            for matchReplace, instances in mappings.items():

                if html.escape(matchReplace[0]) == html.escape(matchReplace[1]):
                    continue
                not_safe = not any(inst['isSafe'] for inst in instances)
                if not_safe:
                    u += 1
                    # continue
                # print(i)
                mapping_id = 'Mapping-{0}'.format(str(i))
                mapping_details = os.path.join(typeChangeFolder, mapping_id + ".html")
                mappingSummary[mapping_id] = {'Match': html.escape(matchReplace[0]),
                                              'Replace': html.escape(matchReplace[1]),
                                              'Instances': len(instances),
                                              'Relevant Instances': len(
                                                  [i for i in instances if i['isRelevant'] != 'Not Relevant']),
                                              'Commits': len({inst['Commit'] for inst in instances}),
                                              'Project': len({inst['Project'] for inst in instances}),
                                              'Link': "<a href=" + mapping_details + ">Link</a>"}
                mappingSummary[mapping_id]['isChange'] = mappingSummary[mapping_id]['Match'] != mappingSummary[mapping_id]['Replace']
                mappingSummary[mapping_id]['isGood'] = mappingSummary[mapping_id]['Commits'] > 1 and mappingSummary[mapping_id]['isChange']
                mappingSummary[mapping_id]['isVeryGood'] = mappingSummary[mapping_id]['Project'] > 1 and mappingSummary[mapping_id]['isChange']
                if mappingSummary[mapping_id]['isVeryGood'] and not not_safe:
                    veryGoodMappingCounter += 1
                if mappingSummary[mapping_id]['isGood'] and not not_safe:
                    goodMappingCounter += 1

                # html = json.loads([i for i in instances['Instance']])
                instances_ = [i for i in instances]
                page = json_to_html(instances_)
                with open(mapping_details, 'w+') as t:
                    t.write(page)
                with open(mapping_details.replace('.html', '.json'), 'w+') as fx:
                    json.dump(instances_, fx)
                i += 1

            if i == 0 and u == 0:
                print(typeChange)
            if i == 0 and u > 0:
                print("----" + str(typeChange))
            commits = set()
            projects = set()
            for matchReplace, instances in mappings.items():
                for instance in instances:
                    commits.add(instance['Commit'])
                    projects.add(instance['Project'])
            commits_no = len(commits)
            projects_no = len(projects)
            mappingSummaryPath = os.path.join(res, "TypeChange" + str(typeChangeID), "MappingSummary" + ".html")


            createHTMLTableFor(str(typeChange), mappingSummary, 'Statement Mappings', mappingSummaryPath)

            with open(mappingSummaryPath.replace('.html','.json'), 'w') as fx:
                json.dump(mappingSummary, fx)
            if i >= 1:
                typeChangeSummary[typeChangeID] = {'Before Type': html.escape(typeChange[0]),
                                                   'After Type': html.escape(typeChange[1]),
                                                   'Good Mappings': goodMappingCounter,
                                                   'Very Good Mappings': veryGoodMappingCounter,
                                                   'No. of Commits': commits_no,
                                                   'No. of Projects': projects_no,
                                                   'Mappings': "<a href=" + mappingSummaryPath + ">" + str(i-u) + "</a>"}

            typeChangeID += 1

        createHTMLTableFor(str(typeChange), typeChangeSummary, 'Type Change Summary',
                           os.path.join(res, "TypeChangeSummary.html"))

        with open(os.path.join(res, 'TypeChangeSummary.json'), 'w') as fx:
            json.dump(list(typeChangeSummary.values()), fx)

        with open(os.path.join(res, 'typeChangeIds.json'), 'w') as fx:
            json.dump(type_change_ids, fx)