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
        col_types = '[\'string\',\'string\', \'number\', \'number\',\'number\', \'number\', \'number\']'
        Title = 'TypeChange Summary'
        body = []
        for k, v1 in mappingSummary.items():
            body.append([str(v1[c]) for c in colNames])
        apply_template_to(htmlPage, (colNames, body, Title, col_types, ".."))


excludeTcs = [('java.lang.String', 'java.util.Optional<:[tar]>')]

res = "/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/ResultAnalysis"
if not os.path.exists(res):
    os.mkdir(res)

type_change_ids = {}
with open(os.path.join(parent(res), "newRun.jsonl")) as c:
    with open(os.path.join(parent(res), "output1.jsonl")) as c1:
        lines = c.readlines()
        lines.extend(c1.readlines())
        mappings = [json.loads(l) for l in lines if l != '\n']
        tcTemplate_mapping = {}

        for m in mappings:
            tcTemplate_mapping.setdefault((m['BeforeTypeTemplate'], m['AfterTypeTemplate']),
                                          {}).setdefault((m['Match'], m['Replace']), []).append(m['Instance'])

        typeChangeID = 0
        typeChangeSummary = {}
        for typeChange, mappings in tcTemplate_mapping.items():
            if typeChange in excludeTcs:
                continue
            i = 0
            type_change_ids[typeChangeID] = typeChange
            mappingSummary = {}
            typeChangeFolder = os.path.join(res, "TypeChange" + str(typeChangeID))
            if not os.path.exists(typeChangeFolder):
                os.mkdir(typeChangeFolder)
            veryGoodMappingCounter = 0
            goodMappingCounter = 0
            # commits = set()
            mapping_id_tracker = {}
            for matchReplace, instances in mappings.items():
                print(i)
                mapping_id = 'Mapping-' + str(i)
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





                if mappingSummary[mapping_id]['isVeryGood']:
                    veryGoodMappingCounter += 1
                if mappingSummary[mapping_id]['isGood']:
                    goodMappingCounter += 1
                # html = json.loads([i for i in instances['Instance']])
                instances_ = [i for i in instances]
                page = json_to_html(instances_)
                with open(mapping_details, 'w+') as t:
                    t.write(page)
                with open(mapping_details.replace('.html', '.json'), 'w+') as fx:
                    json.dump(instances_, fx)

                i += 1


            commits = set()
            projects = set()
            for matchReplace, instances in mappings.items():
                for i in instances:
                    commits.add(i['Commit'])
                    projects.add(i['Project'])
            commits_no = len(commits)
            projects_no = len(projects)
            mappingSummaryPath = os.path.join(res, "TypeChange" + str(typeChangeID), "MappingSummary" + ".html")


            createHTMLTableFor(str(typeChange), mappingSummary, 'Statement Mappings', mappingSummaryPath)

            with open(mappingSummaryPath.replace('.html','.json'), 'w') as fx:
                json.dump(mappingSummary, fx)

            typeChangeSummary[typeChangeID] = {'Before Type': html.escape(typeChange[0]),
                                               'After Type': html.escape(typeChange[1]),
                                               'Good Mappings': goodMappingCounter,
                                               'Very Good Mappings': veryGoodMappingCounter,
                                               'No. of Commits': commits_no,
                                               'No. of Projects': projects_no,
                                               'Mappings': "<a href=" + mappingSummaryPath + ">" + str(
                                                   len(mappings)) + "</a>"}

            typeChangeID += 1

        createHTMLTableFor(str(typeChange), typeChangeSummary, 'Type Change Summary',
                           os.path.join(res, "TypeChangeSummary.html"))

        with open(os.path.join(res, 'typeChangeIds.json'), 'w') as fx:
            json.dump(type_change_ids, fx)