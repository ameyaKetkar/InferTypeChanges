from os import listdir
import json
from collections import namedtuple as nt
import os

import statistics

path_to_resolved_commits = '/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/ResolvedResponseExperiment'

TypeChange = nt('TypeChange', ['source_type', 'target_type'])


def generate_input():
    typeChange_commit = {}
    for c in listdir(path_to_resolved_commits):
        try:
            with open(os.path.join(path_to_resolved_commits, c), 'r') as f:
                resolved_commit = json.load(f)
                tc_template = resolved_commit['resolvedTypeChanges']
                commit = resolved_commit['commits'][0]['sha1']
                url = resolved_commit['commits'][0]['repository']
                pr = url.replace("https://github.com/", "").replace(".git", "").split('/')[1]
                for template in tc_template:
                    typeChange_commit.setdefault((template['_2']['_1'], template['_2']['_2']), set()).add(
                        (pr, url, commit))
        except:
            print('could not analyze', c)

    qtc = [x[1] for x in queryTypeChanges]
    typeChange_commit = {k: v for k, v in
                         sorted(typeChange_commit.items(), key=lambda item: len({x[1] for x in item[1]}),
                                reverse=True) if cleanup(k) and len({x[1] for x in v}) > 3 if k in qtc}
    print(len(typeChange_commit))
    for k in typeChange_commit.keys():
        print(k[0]+'->'+k[1])

    csv = str.join("\n", [str.join(',', [c1[0], c1[1], c1[2]]) for c1 in {c for k, v in typeChange_commit.items() for c in v}])
    with open('/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/vPopularTCCommitsMore.txt', 'w+') as f:
        f.write(csv)
    print()


def cleanup(k):
    return k[0] != k[1] and \
           k[0] not in ['var', 'val', 'java.lang.Void', 'java.lang.Object', 'void'] \
           and k[1] not in ['var', 'val', 'java.lang.Void', 'java.lang.Object', 'void'] \
           and (k[0]!=':[[v0]]' and k[1] != ':[v0]') \
           and len(k[0]) > 1 and len(k[1]) > 1 and k[0] != k[1] \
           and not k[1].endswith(k[0]) and not k[0].endswith(k[1]) \
           and '?' not in k[0] and '?' not in k[1] \
           and not k[1].startswith(k[0] + "<") and not k[0].startswith(k[1] + "<") \
           and not any(x in k[0] or x in k[1] for x in
                       ['java.lang.Object, java.lang.Number','Exception', 'java.lang.Exception', 'java.lang.RuntimeException',
                        'java.lang.Throwable', 'IOException', 'FileNotFoundException'])


queryTypeChanges = [
    # ('flt2dbl',  ('float', 'double')),
    # ('I2L', ('int', 'long')),
    # ('dbl2int', ('double', 'int')),
    # ('IUnBox', ('java.lang.Integer', 'int')),
    # ('ILUnBox', ('java.lang.Boolean', 'boolean')),
    # ('BUnBox', ('java.lang.Long', 'long')),
    # ('F2P', ('java.io.File', 'java.nio.file.Path')),
    # ('P2F', ('java.nio.file.Path', 'java.io.File')),
    # ('Str2P', ('java.lang.String', 'java.nio.file.Path')),
    # ('S2UUID', ('java.lang.String', 'java.util.UUID')),
    # ('Str2Ptr', ('java.lang.String', 'java.util.regex.Pattern')),
    # ('2O', (':[v0]', 'java.util.Optional<:[v0]>')),
    # ('L2S', ('java.util.List<:[v0]>', 'java.util.Set<:[v0]>')),
    # ('2L', (':[v0]', 'java.util.List<:[v0]>')),
    # ('L2IL', ('java.util.List<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>')),
    # ('Is2IL', ('com.google.common.collect.ImmutableList<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>')),
    # ('SB2SBF', ('java.lang.StringBuffer', 'java.lang.StringBuilder')),
    # ('ToAR', (':[v0]', 'java.util.concurrent.atomic.AtomicReference<:[v0]>')),
    # ('L2AL', ('long', 'java.util.concurrent.atomic.AtomicLong')),
    # ('I2AI', ('int', 'java.util.concurrent.atomic.AtomicInteger')),
    # ('AI2AL', ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.AtomicInteger')),
    # ('M2CM', ('java.util.Map<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v1],:[v0]>')),
    # ('TF2F', ('org.junit.rules.TemporaryFolder', 'java.io.File')),
    # ('CS2CD', ('rx.subscriptions.CompositeSubscription', 'io.reactivex.disposables.CompositeDisposable')),
    # ('L2L', ('org.apache.commons.logging.Log', 'org.slf4j.Logger')),
    # ('GM2HG', ('org.apache.commons.httpclient.methods.GetMethod', 'org.apache.http.client.methods.HttpGet')),
    # ('AL2LA', ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.LongAdder')),
    # ('AI2LA', ('java.util.concurrent.atomic.AtomicInteger', 'java.util.concurrent.atomic.LongAdder')),
    # ('Url2Uri', ('java.net.URL', 'java.net.URI')),
    # ('StackToDeque', ('java.util.Stack<:[v0]>', 'java.util.Deque<:[v0]>')),
    # ('L2LinkedL', ('java.util.List<:[v0]>', 'java.util.LinkedList<:[v0]>')),
    # ('FuncToPred', ('java.util.function.Function<:[v0],java.lang.Boolean>', 'java.util.function.Predicate<:[v0]>')),
    # ('LinkedLtoDq', ('java.util.LinkedList<:[v0]>', 'java.util.Deque<:[v0]>')),
    # ('M2MC', ('com.mongodb.Mongo', 'com.mongodb.MongoClient')),
    # ('StrArrToLS', ('java.lang.String[]', 'java.util.List')),
    # ('Fn2ToIntFn', ('java.util.function.Function<:[v0],java.lang.Integer>', 'java.util.function.ToIntFunction<:[v0]>')),
    # ('Fn2ToLongFn',
    #  ('java.util.function.Function<:[v0],java.lang.Integer>', 'java.util.function.ToLongFunction<:[v0]>')),
    # ('Fn2ToDblFn',
    #  ('java.util.function.Function<:[v0],java.lang.Integer>', 'java.util.function.ToDoubleFunction<:[v0]>')),
    # ('2IntList', ('java.util.List<java.lang.Integer>', 'com.google.protobuf.Internal.IntList')),
    # ('2BoolSup', ('java.util.function.Supplier<java.lang.Boolean>', 'java.util.function.BooleanSupplier')),
    # ('Op2IOp', ('java.util.Optional<java.lang.Integer>', 'java.util.OptionalInt')),
    # ('QtoBQ', ('java.util.Queue<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>')),
    # ('Rndm2SRndm', ('java.util.Random', 'java.security.SecureRandom')),
    # ('Sup2IntSup', ('java.util.function.Supplier<java.lang.Integer>', 'java.util.function.IntSupplier')),
    # ('Bytarr2BytBuf', ('byte[]', 'java.nio.ByteBuffer')),
    # ('ChBuf2BytBuf', ('org.jboss.netty.buffer.ChannelBuffer', 'io.netty.buffer.ByteBuf')),
    # ('IN2ReddisonN', ('io.netty.util.concurrent.Promise<:[v0]>', 'org.redisson.misc.RPromise<:[v0]>')),
    # ('Str2Fl', ('java.lang.String', 'java.io.File')),
    # ('l2BI', ('long', 'java.math.BigInteger')),
    # ('PM2HP', ('org.apache.commons.httpclient.methods.PostMethod', 'org.apache.http.client.methods.HttpPost')),
    # ('PutM2HPut', ('org.apache.commons.httpclient.methods.PutMethod', 'org.apache.http.client.methods.HttpPut')),
    # ('HM2Hget', ('org.apache.commons.httpclient.HttpMethod', 'org.apache.http.client.methods.HttpGet')),
    # ('HM2Hpost', ('org.apache.commons.httpclient.HttpMethod', 'org.apache.http.client.methods.HttpPost')),
    # ('Resp2CloseableResp', ('org.apache.http.HttpResponse', 'org.apache.http.client.methods.CloseableHttpResponse')),
    # ('M2Prop', ('java.util.Map<java.lang.String,java.lang.String>', 'java.util.Properties')),
    # ('lng2Dur', ('long', "java.time.Duration")),
    # ('bool2AtomicBool', ('boolean', 'java.util.concurrent.atomic.AtomicBoolean')),
    # ('Op2Oplng', ('java.util.Optional<java.lang.Long>', 'java.util.OptionalLong')),
    # ('Fn2IntUn',
    #  ('java.util.function.Function<java.lang.Integer,java.lang.Integer>', 'java.util.function.IntUnaryOperator')),
    # ('Fn2DblUn',
    #  ('java.util.function.Function<java.lang.Double,java.lang.Double>', 'java.util.function.DoubleUnaryOperator')),
    # (
    # 'Fn2LngUn', ('java.util.function.Function<java.lang.Long,java.lang.Long>', 'java.util.function.LongUnaryOperator')),
    # ("HM2HUR", ("org.apache.commons.httpclient.HttpMethod", "org.apache.http.client.methods.HttpUriRequest")),
    # ('2SUP', (":[v0]", "java.util.function.Supplier<:[v0]>")),
    # ('Str2Inet', ('java.lang.String', 'java.net.InetSocketAddress')),
    # ('File2andrdURI', ('java.io.File', 'android.net.Uri')),
    # ('lngToInst', ('long', 'java.time.Instant')),
    # ('dateToInst', ('java.util.Date', 'java.time.Instant')),
    # ('joda2ZonedDT', ('org.joda.time.DateTime', 'java.time.ZonedDateTime')),
    # ('util2TimeDate', ('java.util.Date', 'java.time.LocalDate')),
    # ('int2Duration', ('int', 'java.time.Duration')),
    # ('long2Duration', ('long', 'java.time.Duration')),
    # ('util2time', ('java.util.TimeZone', 'java.time.ZoneId')),
    # ('date2ZDT', ('java.util.Date', 'java.time.ZonedDateTime')),
    # ('calToZDT', ('java.util.Calendar', 'java.time.ZonedDateTime')),
    # ('Sdf2Dtf', ('java.text.SimpleDateFormat', 'java.time.format.DateTimeFormatter')),
    # ('casToInetJava', ('org.apache.cassandra.locator.InetAddressAndPort', 'java.net.InetSocketAddress')),
    # ('Map2Bvar', ('java.util.Map<java.lang.String,com.facebook.presto.spi.type.Type>','com.facebook.presto.metadata.BoundVariables')),
    # ('List2Page', ('java.util.List<:[v0]>', 'org.eclipse.che.api.core.Page<:[v0]>')),
    # ('Q2Dq', ('java.util.Queue<:[v0]>', 'java.util.Deque<:[v0]>')),
    # ('DbCollec2Mongo', ('com.mongodb.DBCollection', 'com.mongodb.client.MongoCollection<org.bson.Document>')),
    # ('Dbo2Doc', ('com.mongodb.DBObject', 'org.bson.Document')),
    # ('json2gson', ('org.json.JsonObject', 'com.google.gson.JsonObject')),
    # ('File2hadoopPath', ('java.io.File', 'org.apache.hadoop.fs.Path')),
    # ('Callable2Sup', ('java.util.concurrent.Callable<:[v0]>', 'java.util.function.Supplier<:[v0]>')),
    # ('BuffOS2OS', ('java.io.BufferedOutputStream', 'java.io.OutputStream')),
    # ('joda2DF', ('org.joda.time.format.DateTimeFormatter', 'org.elasticsearch.common.time.DateFormatter')),
    # ('DbRuleToDbAPI', ('org.neo4j.test.rule.DatabaseRule', 'org.neo4j.kernel.internal.GraphDatabaseAPI')),
    # ('Str2Int',('java.lang.String', 'int')),
    # ('Str2ByteArr', ('java.lang.String', 'byte[]')),
    # ('FileIS2IS',('java.io.FileInputStream', 'java.io.InputStream')),
    # ('SwapMap',('Map.Entry<:[v1],:[v0]>', 'Map.Entry<:[v0],:[v1]>'))
]

generate_input()
