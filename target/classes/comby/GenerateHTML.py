import itertools
import json
import os
from os.path import dirname as parent

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


output = "/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output"

with open(os.path.join(output, "PopularTypeChanges.json")) as p:
    data = p.read()
    typeChangeFileNames = json.loads(data)

overallAnalysis = {}
for k, v in typeChangeFileNames.items():
    curr_folder = os.path.join(output, v.replace('.jsonl', ''))
    if not os.path.exists(curr_folder):
        os.mkdir(curr_folder)
    mappingSummary = {}
    mappingDetails = {}
    # collectedMappings = {}


    # collected_mappings = {}
    # with open(os.path.join(output, v)) as f:
    #     lines = f.readlines()
    #     for i in range(0, len(lines)):
    #         try:
    #             data = json.loads(lines[i])
    #             collected_mappings[Mapping(data['Match'], data['Replace'])] = data['Instances']
    #             print()
    #         except:
    #             print('Could not read line number ', str(i))

    # collected_mappings.sort(key=lambda m: len(m['Instances']), reverse=True)
    # one instance is a part of more than one templates

    # groupedByInstances = [g for g in itertools.groupby([(mr,i) for mr, instances in collected_mappings.items() for i in instances], key= lambda e:e[1])]


    with open(os.path.join(output, v)) as f:
        lines = f.readlines()
        for i in range(0, len(lines)):
            try:
                print(i)
                mapping_id = 'Mapping-' + str(i)
                data = json.loads(lines[i])
                mapping_details = os.path.join(curr_folder, mapping_id + ".html")
                mappingSummary[mapping_id] = {'Match': data['Match'], 'Replace': data['Replace'],
                                              'Instances': len(data['Instances']),
                                              'Commits': len({inst['Commit'] for inst in data['Instances']}),
                                              'Project': len({inst['Project'] for inst in data['Instances']}),
                                              'Link': "<a href=" + mapping_details + ">Link</a>"}
                # html = json.loads(data['Instances'])
                page = json_to_html(data['Instances'])
                with open(mapping_details, 'w+') as t:
                    t.write(page)
            except:
                print(lines[i])

    colNames = ['Match', 'Replace', 'Instances', 'Commits', 'Project', 'Link']
    col_types = '[\'string\',\'string\', \'number\', \'number\',\'number\',\'string\']'
    template = 'GeneralDicts'
    Title = 'Mapping Summary for ' + str(k)

    body = []
    for k, v1 in mappingSummary.items():
        body.append([str(v1[c]) for c in colNames])

    htmlPage = os.path.join(output, v.replace(".jsonl", ".html"))
    apply_template_to(htmlPage, (colNames, body, Title, col_types, "."))
