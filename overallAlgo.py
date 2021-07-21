# python 



def tcInfer(commits):
    rules = {}
    for commit in commits:
        tcis, rfctrs = RMiner(commit)
        for tci in tcis:
            for adp in tci.adaptations:
                adp = preProcess(adp, tci, rfctrs)
                rs = filter(lambda x: isRelevant(x, tci),Infer(adp))
                addToRules(rules, tci.typeChangeKind, rs)
    return rules