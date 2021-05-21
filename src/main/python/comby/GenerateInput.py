from os.path import join, exists
from comby.RW import readAll
from Models.Models.PrettyPrint import pretty
from collections import namedtuple as nt
import json

TypeChange = nt('TypeChange', ['source_type', 'target_type'])

CommitInfo = nt('CommitInfo', ['project_name', 'project_url', 'sha'])
TypeChangeCommits = nt('TypeChangeCommit', ['source_type', 'target_type', 'commitInfo'])

typeChange_projects = {}
typeChange_commits = {}
commits_json = '/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/TypeChangeCommits.json'
miner = '/Users/ameya/Research/TypeChangeStudy/TypeChangeMiner'


def get_type_changes(change_miner):
    projects = readAll("Projects", "Project", protos=join(change_miner, 'Input/ProtosOut/'))
    for project in projects:
        for cmt in readAll("TypeChangeCommit_" + project.name, "TypeChangeCommit", protos=join(change_miner, 'Output')):
            for tc in cmt.typeChanges:
                typeChange = TypeChange(pretty(tc.b4), pretty(tc.aftr))
                typeChange_projects.setdefault(typeChange, set()).add((project.name))
                typeChange_commits.setdefault(typeChange, set()).add(CommitInfo(project.name, project.url, cmt.sha))

    unpopular_type_changes = [k for k, v in typeChange_projects.items() if len(v) == 1]
    output = []
    for tc, commitInfo in typeChange_commits.items():
        if tc not in unpopular_type_changes:
            output.append([tc.source_type, tc.target_type, [c for c in commitInfo]])
    return output


def getTypeChangeCommitJson():
    if not exists(commits_json):
        commitsTypeChange = get_type_changes(miner)
        with open(commits_json, 'w+') as f:
            f.write(json.dumps(commitsTypeChange))
            return commitsTypeChange
    else:
        with open(commits_json, 'r') as f:
            d = f.read()
            return json.loads(d)


def generateInputFor(typeChanges):
    typechange_commitInfo = getTypeChangeCommitJson()
    csv = ''
    for t in typeChanges:
        ci = [x for x in typechange_commitInfo if x[0] == t.source_type and x[1] == t.target_type]
        if len(ci) > 0:
            csv = csv + '\n' + str.join('\n', {str.join(',', [c[0], c[1], c[2]]) for c in ci[0][2]})
    with open('/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/input.txt', 'w+') as f:
        f.write(csv)


queryTypeChanges = [
    # TypeChange("int", "long"),
    # TypeChange("java.io.File", "java.nio.file.Path"),
    # TypeChange("java.util.List", "java.util.Set"),
    # TypeChange("java.lang.String", "java.util.UUID"),
    # TypeChange("java.lang.String", "java.net.URI"),
    # TypeChange("java.lang.String", "java.util.regex.Pattern"),
    # TypeChange("java.lang.String", "java.util.Set<java.lang.String>"),
    # TypeChange("java.lang.String", "java.util.File"),
    # TypeChange("java.net.URL", "java.net.URI"),
    # TypeChange("java.net.URI", "java.net.URL"),
    # TypeChange("java.lang.String", "java.util.Optional<java.lang.String>"),
    # TypeChange("long", "java.time.Duration"),
    # # TypeChange("long", "java.time.Instant"),
    # # TypeChange("java.lang.Long", "java.time.Duration"),
    # TypeChange("java.util.Date", "java.time.Instant"),
    # TypeChange("java.util.Date", "java.time.LocalDate"),
    # # TypeChange("java.util.List", "java.util.Set"),
    # # TypeChange("java.util.Set", "com.google.common.collect.ImmutableSet"),
    # # TypeChange("java.util.Map", "com.google.common.collect.ImmutableMap"),
    # # TypeChange("java.util.Stack", "java.util.Deque"),
    #
    # TypeChange("java.util.function.Function", "java.util.function.DoubleUnaryOperator"),
    # TypeChange("java.util.function.Function", "java.util.function.IntUnaryOperator"),
    # TypeChange("java.util.function.Function", "java.util.function.LongUnaryOperator"),
    # TypeChange("java.util.function.Function", "java.util.function.IntPredicate"),
    # TypeChange("java.util.function.Predicate", "java.util.function.IntPredicate"),
    # TypeChange("java.util.function.Function", "java.util.function.Predicate"),
    # TypeChange("java.util.function.Function", "java.util.function.LongPredicate"),
    # TypeChange("java.util.function.Predicate", "java.util.function.LongPredicate"),
    # TypeChange("java.util.Optional", "java.util.OptionalInt"),
    # TypeChange("java.text.SimpleDateFormat", "java.time.format.DateTimeFormatter"),
    TypeChange("java.util.Map<java.lang.String, java.lang.String>", "java.util.Properties")
    # TypeChange("org.json.JSONObject", "com.google.gson.JsonObject")
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

generateInputFor(queryTypeChanges)
