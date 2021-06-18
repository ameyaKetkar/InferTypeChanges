import itertools
from os.path import exists, join
from os import listdir
import json

trackers = '/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Output/trackers'

def chunks(x):
    return '/Users/ameya/Research/TypeChangeStudy/HttpServer/Dataset/chunk'+str(x)+'.csv'

projects = {}
for c in [1,2,3,4,5]:
    with open(chunks(c), 'r') as f:
        pr = {l.split(',')[0]: l.split(',')[1] for l in f.readlines()}
        projects = {**projects, **pr}



time_out_commits = []
for tracker in listdir(trackers):
    with open(join(trackers, tracker), 'r') as f:
        commits = [[c.split(',')[0], c.split(',')[1]] for c in f.readlines() if 'Timeout' in c]
        time_out_commits.extend(commits)
timedout_commits = {k +"," +projects[k]: [g[0] for g in grp]for k, grp in itertools.groupby(time_out_commits, key=lambda x:x[1])}
with open(join(trackers, 'timeout_commits.json'), 'w+') as f:
    json.dump(timedout_commits,f)

