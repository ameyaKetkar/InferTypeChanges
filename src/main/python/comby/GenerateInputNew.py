from os import listdir
import json
from collections import namedtuple as nt
import os
from tkintertable.Tables import TableCanvas
from tkintertable.TableModels import TableModel
import tkinter as tk

path_to_resolved_commits = '/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/ResolvedResponses'

TypeChange = nt('TypeChange', ['source_type', 'target_type'])
queryTypeChanges = []

# master = tk.Tk()
# tframe = tk.Frame(master)
# tframe.pack()
# data = {'rec1': {'col1': 99.88, 'col2': 108.79, 'label': 'rec1'},
#         'rec2': {'col1': 99.88, 'col2': 108.79, 'label': 'rec2'}
#         }
# # model = TableModel()
# table = TableCanvas(tframe, data=data, cellwidth=60, cellbackgr='#e3f698',
#                     thefont=('Arial', 12), rowheight=18, rowheaderwidth=30,
#                     rowselectedcolor='yellow', editable=True)
# tframe.pack()
# # model = table.model
# # model.importDict(data)
# # table.redraw()
# table.show()
# print()


def generate_input():
    typeChange_commit = {}
    for c in listdir(path_to_resolved_commits):
        with open(os.path.join(path_to_resolved_commits, c), 'r') as f:
            resolved_commit = json.load(f)
            tc_template = resolved_commit['resolvedTypeChanges']
            commit = resolved_commit['commits'][0]['sha1']
            url = resolved_commit['commits'][0]['repository']
            pr = url.replace("https://github.com/", "").replace(".git", "").split('/')[1]
            for template in tc_template:
                typeChange_commit.setdefault((template['_2']['_1'], template['_2']['_2']), set()).add((pr, url, commit))
            print()

    typeChange_commit = {k: v for k, v in sorted(typeChange_commit.items(), key=lambda item: len(item[1]),
                                                 reverse=True) if k in queryTypeChanges}

    csv = str.join("\n", [str.join(',', [c[0], c[1], c[2]]) for k, v in typeChange_commit.items() for c in v])
    with open('/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/input.txt', 'w+') as f:
        f.write(csv)
    print()


queryTypeChanges = [
    # TypeChange("int", "long"),
    ("java.io.File", "java.nio.file.Path"),
    (":[v0]", "java.util.Optional<:[v0]>"),
    ("java.util.List<:[v0]>", "java.util.Set<:[v0]>"),
    (":[v0]", "java.util.List<:[v0]>"),
    # TypeChange("java.lang.String", "java.util.UUID"),
    # TypeChange("java.lang.String", "java.net.URI"),
    # TypeChange("java.lang.String", "java.util.regex.Pattern"),
    # TypeChange("java.lang.String", "java.util.Set<java.lang.String>"),
    # TypeChange("java.lang.String", "java.util.File"),
    ("java.net.URL", "java.net.URI"),
    # ("java.net.URI", "java.net.URL"),
    # # TypeChange("java.lang.String", "java.util.Optional<java.lang.String>"),
    ("long", "java.time.Duration"),
    ("long", "java.math.BigInteger"),
    ("java.lang.Long", "java.math.BigInteger"),
    ("int", "java.math.BigInteger"),
    ("java.lang.Integer", "java.math.BigInteger"),
    ("long", "java.time.Instant"),
    ("java.lang.Long", "java.time.Duration"),
    ("java.util.Date", "java.time.Instant"),
    # TypeChange("java.util.Date", "java.time.LocalDate"),
    # # # TypeChange("java.util.List", "java.util.Set"),
    # TypeChange("java.util.Set", "com.google.common.collect.ImmutableSet"),
    # # TypeChange("java.util.Map", "com.google.common.collect.ImmutableMap"),
    # TypeChange("java.util.Stack", "java.util.Deque"),
    # # #
    ("java.util.function.Function", "java.util.function.DoubleUnaryOperator"),
    ("java.util.function.Function", "java.util.function.IntUnaryOperator"),
    ("java.util.function.Function", "java.util.function.LongUnaryOperator"),
    ("java.util.function.Function", "java.util.function.IntPredicate"),
    ("java.util.function.Predicate", "java.util.function.IntPredicate"),
    ("java.util.function.Function", "java.util.function.Predicate"),
    ("java.util.function.Function", "java.util.function.LongPredicate"),
    ("java.util.function.Predicate", "java.util.function.LongPredicate"),
    # TypeChange("java.util.Optional", "java.util.OptionalInt"),
    # TypeChange("java.text.SimpleDateFormat", "java.time.format.DateTimeFormatter"),
    # TypeChange("java.util.Map<java.lang.String, java.lang.String>", "java.util.Properties"),
    # TypeChange("org.json.JSONObject", "com.google.gson.JsonObject"),
    # TypeChange("java.util.concurrent.Callable", "java.util.function.Supplier"),
    # TypeChange("java.util.function.Function", "java.util.function.ToDoubleFunction"),
    # TypeChange("java.util.function.Function", "java.util.function.ToIntFunction"),
    # TypeChange("java.util.function.Function", "java.util.function.ToLongFunction"),
    # TypeChange("java.util.function.Function", "java.util.function.Predicate"),
    # TypeChange("java.util.function.Function", "java.util.function.IntPredicate"),
    # TypeChange("java.util.function.Predicate", "java.util.function.IntPredicate"),
    # TypeChange("java.util.Optional", "java.util.OptionalInt"),
    # TypeChange("long", "java.util.concurrent.atomic.AtomicLong"),
    # TypeChange("int", "java.util.concurrent.atomic.AtomicInteger"),
    # TypeChange("java.util.Map", "java.util.concurrent.ConcurrentHashMap"),
    # TypeChange("java.util.concurrent.BlockingQueue", "java.util.Queue"),
    # TypeChange("org.apache.hadoop.hbase.KeyValue", "org.apache.hadoop.hbase.Cell"),
]

generate_input()
