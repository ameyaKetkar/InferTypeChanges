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

GeneralDicts = lambda data: GeneralDictsTemplate.render(cols=data[0], body=data[1], Title=data[2],
                                                        col_types=data[3], css=data[4])

GeneralDictsTemplate = env.get_template("GeneralDictionariesTemplate.html")


def apply_template_to(write_to_file, data):
    with open(os.path.join(write_to_file), 'w+') as fh:
        fh.write(GeneralDicts(data))
        fh.close()


def json_to_html(json_data):
    return json2html.convert(json=json_data, escape=False)

def createHTMLTableFor(typeChange, mappingSummary, forWhat, htmlPage):
    if forWhat == 'Statement Mappings':
        colNames = ['Match', 'Replace', 'Instances','Relevant Instances', 'Commits', 'Project', 'Link']
        col_types = '[\'string\',\'string\', \'number\', \'number\', \'number\',\'number\',\'string\']'
        template = 'GeneralDicts'
        Title = 'Mapping Summary for ' + str(typeChange)
        body = []
        for k, v1 in mappingSummary.items():
            body.append([str(v1[c]) for c in colNames])

        apply_template_to(htmlPage, (colNames, body, Title, col_types, "../.."))
    if forWhat == 'Type Change Summary':
        colNames = ['Before Type', 'After Type', 'Mappings']
        col_types = '[\'string\',\'string\', \'number\']'
        template = 'GeneralDicts'
        Title = 'TypeChange Summary'
        body = []
        for k, v1 in mappingSummary.items():
            body.append([str(v1[c]) for c in colNames])
        apply_template_to(htmlPage, (colNames, body, Title, col_types, ".."))

excludeTcs = [('java.lang.String', 'java.util.Optional<:[tar]>')]

res = "/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/ResultsTest"
if not os.path.exists(res):
    os.mkdir(res)


with open(os.path.join(parent(res), "test.jsonl")) as c:
    lines = c.readlines()
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
        mappingSummary = {}
        typeChangeFolder = os.path.join(res, "TypeChange" + str(typeChangeID))
        if not os.path.exists(typeChangeFolder):
            os.mkdir(typeChangeFolder)
        for matchReplace, instances in mappings.items():
            print(i)
            mapping_id = 'Mapping-' + str(i)
            mapping_details = os.path.join(typeChangeFolder, mapping_id + ".html")
            mappingSummary[mapping_id] = {'Match': matchReplace[0], 'Replace': matchReplace[1],
                                          'Instances': len(instances),
                                          'Relevant Instances' : len([i for i in instances if i['isRelevant']]),
                                          'Commits': len({inst['Commit'] for inst in instances}),
                                          'Project': len({inst['Project'] for inst in instances}),
                                          'Link': "<a href=" + mapping_details + ">Link</a>"}
            # html = json.loads([i for i in instances['Instance']])
            page = json_to_html([i for i in instances])
            with open(mapping_details, 'w+') as t:
                t.write(page)

            i += 1

        mappingSummaryPath = os.path.join(res, "TypeChange" + str(typeChangeID), "MappingSummary" + ".html")

        createHTMLTableFor(str(typeChange), mappingSummary, 'Statement Mappings', mappingSummaryPath)

        typeChangeSummary[typeChangeID] = {'Before Type': html.escape(typeChange[0]), 'After Type': html.escape(typeChange[1]),
                                           'Mappings': "<a href=" + mappingSummaryPath + ">" + str(
                                               len(mappings)) + "</a>"}

        typeChangeID += 1

    createHTMLTableFor(str(typeChange), typeChangeSummary, 'Type Change Summary',
                       os.path.join(res, "TypeChangeSummary.html"))
