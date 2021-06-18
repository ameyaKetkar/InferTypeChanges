from os import listdir
import json
from collections import namedtuple as nt
import os
from tkintertable.Tables import TableCanvas
from tkintertable.TableModels import TableModel
import tkinter as tk

path_to_resolved_commits = '/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/ResolvedResponses'

TypeChange = nt('TypeChange', ['source_type', 'target_type'])


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
        try:
            with open(os.path.join(path_to_resolved_commits, c), 'r') as f:
                resolved_commit = json.load(f)
                tc_template = resolved_commit['resolvedTypeChanges']
                commit = resolved_commit['commits'][0]['sha1']
                url = resolved_commit['commits'][0]['repository']
                pr = url.replace("https://github.com/", "").replace(".git", "").split('/')[1]
                for template in tc_template:
                    typeChange_commit.setdefault((template['_2']['_1'], template['_2']['_2']), set()).add((pr, url, commit))
                # print()
        except:
            print('could not analyze',c)




    typeChange_commit = {k: v for k, v in sorted(typeChange_commit.items(), key=lambda item: len(item[1]),
                                                 reverse=True) if k in queryTypeChanges}

    # with open('op.txt', 'w+') as f:
    #     s = str.join('\n',[str(k) + " - " + str(len({x[0] for x in v})) for k,v in typeChange_commit.items()


    #                        and len(k[0]) > 1 and len(k[1])>1 and k[0] != k[1] and not k[1].endswith(k[0])
    #                        and not k[0].endswith(k[1]) and '?' not in k[0] and '?' not in k[1]])
    #     f.write(s)
    #

    csv = str.join("\n", [str.join(',', [c[0], c[1], c[2]]) for k, v in typeChange_commit.items() for c in v])
    with open('/Users/ameya/Research/TypeChangeStudy/InferTypeChanges/Input/inputFP.txt', 'w+') as f:
        f.write(csv)
    print()


queryTypeChanges = [
    ('int', 'long'),
    (':[v0]', 'java.util.Optional<:[v0]>'),
    (':[v0]', 'java.util.List<:[v0]>'),
    ('long', 'int'),
    ('java.util.Optional<:[v0]>', ':[v0]'),
    ('java.util.List<:[v0]>', 'java.util.Set<:[v0]>'),
    ('boolean', 'int'),
    # ('java.util.List<:[v0]>', 'java.util.Collection<:[v0]>'),

    # ('java.util.ArrayList<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.lang.Integer', 'int'),
    ('java.util.List<:[v0]>', ':[v0]'),
    ('java.lang.Boolean', 'boolean'),
    ('java.lang.Long', 'long'),

    # (':[[v0]]', ':[v0]'),
    ('int', 'java.lang.Integer'),
    ('boolean', 'java.lang.Boolean'),
    ('java.util.Set<:[v0]>', 'java.util.List<:[v0]>'),
    ('int', 'java.lang.String'),
    ('java.util.Collection<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.lang.String', 'int'),
    ('java.lang.StringBuffer', 'java.lang.StringBuilder'),
    ('long', 'java.lang.Long'),

    ('java.util.List<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),

    (':[v0]', 'java.util.function.Supplier<:[v0]>'),
    ('int', 'boolean'),

    ('java.lang.String', 'java.io.File'),
    ('java.lang.String', 'java.nio.file.Path'),
    ('java.io.File', 'java.nio.file.Path'),
    ('boolean', 'java.lang.String'),
    ('java.lang.Iterable<:[v0]>', 'java.util.List<:[v0]>'),
    (':[v0]', 'java.util.concurrent.atomic.AtomicReference<:[v0]>'),
    ('long', 'java.lang.String'),
    ('java.util.Set<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('int', 'java.util.concurrent.atomic.AtomicInteger'),
    ('org.apache.commons.logging.Log', 'org.slf4j.Logger'),

    # ('java.util.Set<:[v0]>', 'java.util.Collection<:[v0]>'),
    # ('java.util.HashMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('int', 'float'),
    ('java.lang.String', 'boolean'),
    ('java.util.List<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    (':[v0]', 'java.util.Set<:[v0]>'),

    ('long', 'double'),
    ('java.lang.String', 'java.lang.CharSequence'),
    (':[v0]', 'java.util.Collection<:[v0]>'),
    ('double', 'long'),
    ('java.util.Collection<:[v0]>', 'java.util.Set<:[v0]>'),
    ('int', 'double'),
    ('com.google.common.base.Optional<:[v0]>', ':[v0]'),
    ('boolean', 'java.util.concurrent.atomic.AtomicBoolean'),
    (':[v0]', 'com.google.common.base.Optional<:[v0]>'),
    ('java.util.List<:[v0]>', 'java.lang.Iterable<:[v0]>'),

    ('java.io.File', 'java.lang.String'),
    ('long', 'java.util.concurrent.atomic.AtomicLong'),
    ('java.lang.String', 'long'),



    ('com.google.common.collect.ImmutableList<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.List<:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),
    ('java.lang.Iterable<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('java.lang.Iterable<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v1],:[v0]>'),
    ('java.util.Collection<:[v0]>', 'java.lang.Iterable<:[v0]>'),
    (':[v0]', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('boolean', 'long'),


    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.collect.ImmutableMap<:[v1],:[v0]>'),

    ('double', 'java.lang.Double'),
    ('java.util.HashSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('java.lang.Double', 'double'),
    ('double', 'int'),
    ('float', 'int'),
    ('java.lang.Integer', 'java.lang.Long'),
    ('java.util.function.Supplier<:[v0]>', ':[v0]'),
    (':[v0]', 'org.springframework.beans.factory.ObjectProvider<:[v0]>'),
    (':[v0]', 'java.util.concurrent.CompletableFuture<:[v0]>'),

    (':[v0]', 'java.util.Map<java.lang.String,:[v0]>'),

    ('com.google.common.collect.ImmutableList<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('long', 'java.time.Duration'),
    ('java.util.concurrent.atomic.AtomicInteger', 'int'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'boolean'),
    ('java.lang.String', 'java.net.URI'),
    ('double', 'float'),
    ('java.util.concurrent.atomic.AtomicReference<:[v0]>', ':[v0]'),
    ('java.lang.String', 'java.lang.Integer'),
    ('java.nio.file.Path', 'java.lang.String'),


    ('java.lang.Long', 'java.lang.String'),
    (':[v0]', 'java.lang.Iterable<:[v0]>'),
    ('java.util.List<:[v0]>', 'java.util.stream.Stream<:[v0]>'),
    ('java.util.Map<java.lang.String,:[v0]>', ':[v0]'),
    ('java.lang.Integer', 'java.lang.String'),
    ('java.lang.String', 'java.util.regex.Pattern'),
    ('java.lang.CharSequence', 'java.lang.String'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.List<:[v0]>'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('long', 'boolean'),

    ('java.util.LinkedList<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.Set', 'java.util.Set<java.lang.String>'),

    ('java.util.Collection<:[v0]>', ':[v0]'),
    ('com.google.common.collect.ImmutableMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('int', 'byte'),
    ('com.google.common.collect.ImmutableList<:[v0]>', 'java.lang.Iterable<:[v0]>'),
    ('java.lang.Iterable<:[v0]>', ':[v0]'),
    ('java.lang.Long', 'java.lang.Double'),
    ('java.util.Map', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('java.util.Collection<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('float', 'double'),

    ('java.util.concurrent.atomic.AtomicLong', 'long'),
    ('java.util.Set<:[v0]>', ':[v0]'),
    ('byte', 'int'),

    ('java.lang.Class', 'java.lang.Class<T>'),
    ('java.util.concurrent.CompletableFuture<:[v0]>', ':[v0]'),
    ('java.util.List<:[v0]>', 'java.util.LinkedList<:[v0]>'),
    ('java.lang.Double', 'java.lang.Long'),

    ('java.lang.String', 'org.jooq.Name'),
    ('java.lang.Long', 'java.lang.Integer'),
    ('java.util.concurrent.Executor', 'java.util.concurrent.ExecutorService'),
    ('java.util.Set<:[v0]>', 'java.lang.Iterable<:[v0]>'),
    ('short', 'int'),
    (':[v0]', 'java.lang.ThreadLocal<:[v0]>'),
    ('java.lang.String', 'java.net.URL'),
    ('java.lang.String', 'java.lang.Long'),

    ('java.util.concurrent.ConcurrentMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),

    ('java.lang.Iterable<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('java.lang.String', 'java.lang.StringBuilder'),

    ('java.util.Stack<:[v0]>', 'java.util.Deque<:[v0]>'),

    ('java.util.List<:[v0]>', 'java.util.Queue<:[v0]>'),
    ('java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.io.FileInputStream', 'java.io.InputStream'),

    ('java.util.concurrent.atomic.AtomicInteger', 'java.util.concurrent.atomic.AtomicLong'),
    ('ImmutableList.Builder<:[v0]>', 'ImmutableSet.Builder<:[v0]>'),
    ('java.lang.Iterable<:[v0]>', 'java.util.Set<:[v0]>'),
    ('java.util.stream.Stream<:[v0]>', 'java.util.List<:[v0]>'),

    ('java.lang.String', 'org.elasticsearch.common.settings.Setting<java.lang.Boolean>'),
    ('java.net.URI', 'java.lang.String'),
    (':[v0]', 'com.google.common.util.concurrent.ListenableFuture<:[v0]>'),

    ('long', 'float'),
    ('java.lang.String', 'java.util.UUID'),
    ('java.util.concurrent.ExecutorService', 'java.util.concurrent.ScheduledExecutorService'),

    (':[v0]', 'com.google.inject.Provider<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.HashMap<:[v1],:[v0]>'),
    ('java.util.concurrent.ExecutorService', 'java.util.concurrent.Executor'),
    ('int', 'short'),
    ('float', 'long'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('java.lang.StringBuilder', 'java.lang.StringBuffer'),
    ('java.util.Set<:[v0]>', 'java.util.SortedSet<:[v0]>'),
    ('java.util.Random', 'java.security.SecureRandom'),
    ('java.net.URL', 'java.lang.String'),
    ('java.lang.ref.WeakReference<:[v0]>', ':[v0]'),
    ('java.lang.Iterable<:[v0]>', 'java.util.Iterator<:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.stream.Stream<:[v0]>'),
    ('char', 'java.lang.String'),
    ('java.util.Iterator<:[v0]>', 'java.lang.Iterable<:[v0]>'),
    ('ImmutableMap.Builder<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.lang.Boolean', 'java.lang.String'),
    ('java.lang.String', 'java.nio.charset.Charset'),

    (':[v0]', 'java.util.Map<:[v0],:[v0]>'),
    ('java.lang.Float', 'float'),
    ('java.util.LinkedList<:[v0]>', 'java.util.Deque<:[v0]>'),



    ('java.util.concurrent.ExecutorService', 'java.util.concurrent.ThreadPoolExecutor'),

    ('int', 'java.time.Duration'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'java.lang.Iterable<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),
    ('java.lang.String', 'org.elasticsearch.common.settings.Setting<org.elasticsearch.common.unit.TimeValue>'),
    ('java.net.URL', 'java.net.URI'),
    ('java.nio.file.Path', 'java.io.File'),
    ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.LongAdder'),

    ('char', 'int'),
    ('java.util.Iterator', 'java.util.Iterator<java.lang.String>'),

    ('ImmutableList.Builder<:[v0]>', 'java.util.List<:[v0]>'),

    ('java.util.List<:[v0]>', 'ImmutableList.Builder<:[v0]>'),

    ('java.util.LinkedHashSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('boolean', 'float'),
    ('org.junit.rules.TemporaryFolder', 'java.io.File'),
    ('com.google.common.base.Optional<:[v0]>', 'java.util.Optional<:[v0]>'),

    ('java.util.Optional<:[v0]>', 'java.util.List<:[v0]>'),
    ('boolean', 'double'),


    ('double', 'java.lang.String'),
    (':[v0]', 'java.util.ArrayList<:[v0]>'),
    ('boolean', 'java.util.concurrent.CompletableFuture<java.lang.Boolean>'),
    ('java.util.Date', 'long'),
    (':[v0]', 'com.google.common.base.Supplier<:[v0]>'),
    ('java.lang.String', 'char'),
    ('java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v1],:[v0]>'),
    ('java.util.ArrayList<:[v0]>', 'java.util.Set<:[v0]>'),
    ('org.springframework.context.ApplicationContext', 'org.springframework.context.ConfigurableApplicationContext'),
    ('java.util.Queue<:[v0]>', 'java.util.Deque<:[v0]>'),
    (':[v0]', 'java.util.Iterator<:[v0]>'),
    ('rx.subjects.BehaviorSubject<:[v0]>', 'rx.subjects.PublishSubject<:[v0]>'),
    ('java.util.List<:[v0]>', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.lang.String', 'java.lang.Boolean'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.LinkedHashMap<:[v1],:[v0]>'),
    (':[v0]', 'android.util.SparseArray<:[v0]>'),
    ('com.google.common.collect.ImmutableSortedSet<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('long', 'java.time.Instant'),
    ('double', 'boolean'),
    ('int', 'java.util.Optional<java.lang.Integer>'),
    ('com.google.common.base.Supplier<:[v0]>', ':[v0]'),
    ('java.util.LinkedHashMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.concurrent.ExecutorService', 'com.google.common.util.concurrent.ListeningExecutorService'),
    ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.AtomicInteger'),
    (':[v0]', 'reactor.core.publisher.Mono<:[v0]>'),
    ('com.google.common.collect.ImmutableList<:[v0]>', ':[v0]'),
    ('Map.Entry<java.lang.String,:[v0]>', ':[v0]'),
    ('java.util.Set<java.lang.Integer>', 'java.util.Set<java.lang.String>'),
    ('int', 'java.lang.Long'),

    ('java.lang.Long', 'int'),

    ('int', 'java.util.List<java.lang.Integer>'),
    ('java.util.ArrayList<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('org.joda.time.DateTime', 'java.time.ZonedDateTime'),
    ('java.util.Collection<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('android.app.Activity', 'android.content.Context'),

    (':[v0]', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.util.List<java.lang.String>', 'com.google.protobuf.ProtocolStringList'),

    ('java.lang.String', 'java.lang.Throwable'),
    ('java.lang.Integer', 'long'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.Collection<:[v0]>'),
    ('android.view.View', 'android.widget.TextView'),
    ('java.util.Iterator<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.List<java.io.File>', 'java.util.List<java.nio.file.Path>'),
    ('java.util.Map<java.lang.Integer,:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),

    ('java.net.InetAddress', 'java.lang.String'),
    ('java.util.List<:[v0]>', 'java.util.Deque<:[v0]>'),
    ('long', 'java.util.Optional<java.lang.Long>'),

    ('java.util.LinkedList<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('java.util.function.Supplier<:[v0]>', 'Writeable.Reader<:[v0]>'),
    ('java.util.ArrayList', 'java.util.List'),
    ('java.util.Date', 'java.time.Instant'),
    ('Builder', 'Builder<T>'),
    ('java.util.EnumSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('float', 'java.lang.Float'),
    (':[v0]', 'dagger.Lazy<:[v0]>'),
    ('java.lang.String', 'float'),
    ('byte', 'short'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'com.google.common.collect.ImmutableSortedSet<:[v0]>'),
    ('java.util.List<:[v0]>', 'java.util.Iterator<:[v0]>'),
    ('java.lang.Iterable<:[v0]>', 'java.util.stream.Stream<:[v0]>'),



    ('android.widget.TextView', 'android.view.View'),
    (':[v0]', 'java.lang.ref.WeakReference<:[v0]>'),
    ('java.util.regex.Pattern', 'java.lang.String'),
    ('java.util.concurrent.CountDownLatch', 'java.util.concurrent.CyclicBarrier'),
    ('rx.subscriptions.CompositeSubscription', 'io.reactivex.disposables.CompositeDisposable'),
    ('java.util.List<java.lang.Integer>', 'java.util.List<java.lang.String>'),





    ('boolean', 'byte'),
    ('java.lang.String', 'org.elasticsearch.common.settings.Setting<java.lang.Integer>'),
    ('java.util.Set<:[v0]>', 'java.util.LinkedHashSet<:[v0]>'),
    ('java.io.FileOutputStream', 'java.io.OutputStream'),
    ('java.net.URLClassLoader', 'java.lang.ClassLoader'),



    ('org.apache.flink.runtime.state.AbstractStateBackend', 'org.apache.flink.runtime.state.StateBackend'),
    ('byte', 'char'),
    ('com.google.protobuf.SingleFieldBuilder<:[v0],:[v2],:[v1]>', 'com.google.protobuf.SingleFieldBuilderV3<:[v0],:[v2],:[v1]>'),
    ('com.google.protobuf.RepeatedFieldBuilder<:[v0],:[v2],:[v1]>', 'com.google.protobuf.RepeatedFieldBuilderV3<:[v0],:[v2],:[v1]>'),
    ('com.google.protobuf.GeneratedMessage.BuilderParent', 'com.google.protobuf.GeneratedMessageV3.BuilderParent'),
    ('com.google.protobuf.GeneratedMessage.Builder<:[v0]>', 'com.google.protobuf.GeneratedMessageV3.Builder<:[v0]>'),
    ('com.google.protobuf.GeneratedMessage.FieldAccessorTable', 'com.google.protobuf.GeneratedMessageV3.FieldAccessorTable'),

    ('java.util.List<java.lang.Long>', 'java.util.List<java.lang.String>'),
    ('java.lang.String', 'double'),

    ('java.lang.StringBuilder', 'java.util.StringJoiner'),
    ('org.eclipse.jetty.util.ssl.SslContextFactory', 'SslContextFactory.Server'),
    ('boolean', 'java.lang.Throwable'),
    ('android.view.View', 'android.view.ViewGroup'),
    ('java.util.Queue<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.lang.Integer>'),
    ('java.util.HashMap<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),
    ('java.net.InetAddress', 'java.net.InetSocketAddress'),


    ('java.util.List', 'java.util.List<java.lang.Integer>'),
    ('java.util.stream.Stream<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('java.net.URI', 'java.net.URL'),
    ('org.apache.commons.httpclient.methods.GetMethod', 'org.apache.http.client.methods.HttpGet'),
    ('com.google.common.collect.ImmutableList<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.collect.Multimap<:[v1],:[v0]>'),
    ('java.util.Date', 'java.lang.String'),
    ('java.util.concurrent.ThreadPoolExecutor', 'java.util.concurrent.ExecutorService'),
    ('java.util.List<:[v0]>', 'java.util.SortedSet<:[v0]>'),
    ('java.util.List<java.lang.Integer>', 'java.util.List<java.lang.Long>'),
    ('java.util.Enumeration', 'java.util.Enumeration<java.lang.String>'),
    ('java.lang.String', 'java.net.InetSocketAddress'),
    ('rx.Subscription', 'io.reactivex.disposables.Disposable'),
    ('int', 'java.lang.Number'),
    ('com.google.common.util.concurrent.SettableFuture<:[v0]>', 'java.util.concurrent.CompletableFuture<:[v0]>'),
    ('com.google.common.util.concurrent.ListenableFuture<:[v0]>', 'java.util.concurrent.CompletableFuture<:[v0]>'),
    ('boolean', 'java.util.List<java.lang.String>'),
    ('com.google.protobuf.ProtocolStringList', 'java.util.List<java.lang.String>'),
    ('java.lang.StringBuilder', 'java.lang.String'),


    ('java.lang.String', 'java.net.InetAddress'),
    ('org.springframework.http.ResponseEntity<:[v0]>', ':[v0]'),
    ('java.lang.String', 'com.fasterxml.jackson.databind.JsonNode'),
    ('java.lang.String', 'org.apache.hadoop.hbase.TableName'),

    ('java.util.List<:[v0]>', 'java.util.concurrent.CopyOnWriteArrayList<:[v0]>'),
    ('reactor.core.publisher.Mono<:[v0]>', ':[v0]'),
    ('org.apache.hadoop.hbase.KeyValue', 'org.apache.hadoop.hbase.Cell'),

    ('java.lang.String', 'java.io.InputStream'),
    ('ImmutableSet.Builder<:[v0]>', 'ImmutableList.Builder<:[v0]>'),
    (':[v0]', 'Map.Entry<java.lang.String,:[v0]>'),
    ('int', 'java.util.OptionalInt'),
    ('java.lang.Class<T>', 'java.lang.Class'),

    ('java.util.ArrayList<:[v0]>', ':[v0]'),

    ('java.lang.String', 'android.net.Uri'),
    (':[v0]', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('com.google.protobuf.GeneratedMessage.ExtendableBuilder<:[v1],:[v0]>', 'com.google.protobuf.GeneratedMessageV3.ExtendableBuilder<:[v1],:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.Map<:[v0],:[v0]>'),
    ('org.hibernate.engine.spi.SessionImplementor', 'org.hibernate.engine.spi.SharedSessionContractImplementor'),

    ('java.util.List<java.lang.String>', 'boolean'),
    ('java.net.SocketAddress', 'java.net.InetSocketAddress'),
    ('java.lang.Double', 'java.lang.Number'),
    ('java.util.Set<java.lang.String>', 'boolean'),
    ('boolean', 'java.util.Set<java.lang.String>'),

    (':[v0]', 'javax.inject.Provider<:[v0]>'),

    ('java.util.TreeSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('short', 'byte'),
    ('short', 'java.lang.Short'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'java.util.concurrent.atomic.AtomicInteger'),
    ('int', 'char'),

    ('com.google.common.collect.ImmutableSortedSet<:[v0]>', 'java.util.SortedSet<:[v0]>'),


    ('int', 'org.elasticsearch.index.shard.ShardId'),
    ('org.apache.http.HttpResponse', 'org.apache.http.client.methods.CloseableHttpResponse'),
    ('java.nio.ByteBuffer', 'int'),
    ('java.lang.String', 'java.nio.ByteBuffer'),
    ('java.util.HashSet<:[v0]>', 'java.util.LinkedHashSet<:[v0]>'),
    ('java.lang.ThreadLocal<:[v0]>', ':[v0]'),
    ('com.mongodb.Mongo', 'com.mongodb.MongoClient'),
    ('java.util.Date', 'java.time.LocalDate'),
    ('java.util.Map<java.lang.String,java.lang.String>', 'java.util.Properties'),
    ('java.lang.String', 'java.lang.Class'),
    ('long', 'org.elasticsearch.common.unit.TimeValue'),
    ('java.util.HashMap<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    ('java.util.Deque<:[v0]>', 'java.util.Queue<:[v0]>'),
    ('java.lang.Short', 'short'),
    ('java.util.concurrent.atomic.AtomicInteger', 'java.util.concurrent.atomic.LongAdder'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.Set<:[v0]>'),
    ('com.google.common.base.Function<:[v1],:[v0]>', 'java.util.function.Function<:[v1],:[v0]>'),
    ('boolean', 'java.util.Optional<java.lang.String>'),
    ('Settings.Builder', 'org.elasticsearch.common.settings.Settings'),
    ('java.util.UUID', 'java.lang.String'),

    ('boolean', 'java.util.Optional<java.lang.Boolean>'),

    ('java.lang.Throwable', 'java.lang.String'),
    ('java.util.Map', 'java.util.Map<java.lang.String,java.io.Serializable>'),
    ('java.util.Collection<:[v0]>', 'java.util.stream.Stream<:[v0]>'),
    ('java.lang.String', 'java.util.Locale'),
    ('java.util.SortedSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('java.util.Set<java.io.File>', 'java.util.Set<java.nio.file.Path>'),
    ('java.util.Queue<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.Map<java.net.URL,:[v0]>', 'java.util.Map<java.net.URI,:[v0]>'),
    ('long', 'java.util.concurrent.atomic.LongAdder'),
    ('java.util.Hashtable<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.HashSet<:[v0]>'),
    ('java.util.Map<java.lang.String,java.lang.String>', 'org.elasticsearch.common.settings.Settings'),





    ('java.util.Collection<:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),

    ('java.util.Map<:[v1],:[v0]>', 'java.util.SortedMap<:[v1],:[v0]>'),
    ('org.reactivestreams.Publisher<:[v0]>', 'reactor.core.publisher.Flux<:[v0]>'),
    ('java.io.StringWriter', 'java.io.Writer'),


    ('java.util.Set<:[v0]>', 'java.util.Map<:[v0],java.lang.Boolean>'),
    ('java.util.concurrent.Future<:[v0]>', 'java.util.concurrent.CompletableFuture<:[v0]>'),
    ('java.util.Iterator<:[v0]>', 'java.util.ListIterator<:[v0]>'),
    ('org.apache.http.client.HttpClient', 'org.apache.http.impl.client.CloseableHttpClient'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<java.lang.Integer>'),
    ('java.util.Optional<java.lang.Integer>', 'java.util.OptionalInt'),
    ('android.content.Context', 'android.content.res.Resources'),
    ('java.util.List<:[v0]>', 'java.util.HashSet<:[v0]>'),
    ('javax.lang.model.element.ExecutableElement', 'javax.lang.model.element.Element'),
    ('java.lang.Class<:[v0]>', 'com.google.common.reflect.TypeToken<:[v0]>'),
    ('java.lang.Class<:[v0]>', 'java.util.function.Supplier<:[v0]>'),
    (':[v0]', 'java.lang.ref.SoftReference<:[v0]>'),
    ('java.util.Collection', 'java.util.Collection<java.lang.String>'),
    (':[v0]', 'java.util.stream.Stream<:[v0]>'),
    ('java.io.File', 'java.net.URI'),
    ('java.net.InetSocketAddress', 'java.net.SocketAddress'),
    ('java.util.Vector<:[v0]>', 'java.util.List<:[v0]>'),
    ('ImmutableMap.Builder<:[v1],:[v0]>', 'java.util.HashMap<:[v1],:[v0]>'),
    ('java.lang.Character', 'char'),
    ('android.content.Context', 'android.app.Activity'),
    ('io.netty.channel.ChannelHandlerContext', 'io.netty.channel.Channel'),


    ('org.apache.commons.httpclient.methods.PostMethod', 'org.apache.http.client.methods.HttpPost'),
    ('org.elasticsearch.common.settings.Settings', 'org.elasticsearch.index.IndexSettings'),
    ('java.util.concurrent.ScheduledExecutorService', 'java.util.concurrent.ScheduledThreadPoolExecutor'),

    ('java.util.List<:[v0]>', 'java.util.concurrent.ConcurrentLinkedQueue<:[v0]>'),
    ('java.util.Collection<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    (':[v0]', 'Map.Entry<:[v0],:[v0]>'),
    ('java.io.File', 'java.io.InputStream'),
    ('java.util.LinkedList<:[v0]>', 'java.util.ArrayDeque<:[v0]>'),
    ('byte', 'java.lang.Byte'),
    ('java.util.concurrent.BlockingQueue<:[v0]>', 'java.util.Queue<:[v0]>'),
    ('java.util.Map<java.lang.Long,:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),

    ('java.util.concurrent.CompletableFuture<java.lang.Void>', 'java.util.concurrent.CompletableFuture<java.lang.Boolean>'),

    ('java.util.Map<:[v1],:[v0]>', 'java.util.Map<:[v0],:[v1]>'),
    ('java.lang.String', 'org.elasticsearch.common.settings.Setting<org.elasticsearch.common.unit.ByteSizeValue>'),
    ('boolean', 'java.lang.Thread.State'),
    ('net.bytebuddy.description.field.FieldDescription', 'FieldDescription.InDefinedShape'),
    ('java.lang.Integer', 'java.lang.Number'),
    ('java.util.OptionalInt', 'int'),
    ('byte', 'boolean'),

    ('java.lang.String', 'PackageParser.Package'),
    ('java.util.concurrent.CompletableFuture<:[v0]>', 'java.util.concurrent.CompletionStage<:[v0]>'),

    ('java.util.Properties', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.TreeMap<:[v1],:[v0]>'),
    ('org.jboss.netty.buffer.ChannelBuffer', 'io.netty.buffer.ByteBuf'),
    ('java.util.Iterator<:[v0]>', 'java.util.Collection<:[v0]>'),

    ('java.util.List<java.lang.String>', 'java.util.List<T>'),
    ('org.elasticsearch.common.settings.Settings', 'Settings.Builder'),
    ('java.util.TimeZone', 'java.time.ZoneId'),
    ('java.lang.String', 'java.util.Date'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.io.File>'),


    ('java.lang.Boolean', 'int'),
    ('java.io.DataOutputStream', 'java.io.OutputStream'),

    ('ImmutableList.Builder<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('com.google.common.collect.ImmutableCollection<:[v0]>', 'java.util.Collection<:[v0]>'),



    ('long', 'java.math.BigInteger'),


    ('com.google.protobuf.RepeatedFieldBuilderV3<:[v0],:[v2],:[v1]>', 'com.google.protobuf.RepeatedFieldBuilder<:[v0],:[v2],:[v1]>'),
    ('com.google.protobuf.GeneratedMessageV3.Builder<:[v0]>', 'com.google.protobuf.GeneratedMessage.Builder<:[v0]>'),
    ('com.google.protobuf.SingleFieldBuilderV3<:[v0],:[v2],:[v1]>', 'com.google.protobuf.SingleFieldBuilder<:[v0],:[v2],:[v1]>'),
    ('com.google.protobuf.GeneratedMessageV3.FieldAccessorTable', 'com.google.protobuf.GeneratedMessage.FieldAccessorTable'),
    ('com.google.protobuf.GeneratedMessageV3.BuilderParent', 'com.google.protobuf.GeneratedMessage.BuilderParent'),
    ('com.google.protobuf.GeneratedMessageV3.ExtendableBuilder<:[v1],:[v0]>', 'com.google.protobuf.GeneratedMessage.ExtendableBuilder<:[v1],:[v0]>'),

    ('java.util.List<:[v0]>', 'java.util.Optional<:[v0]>'),
    ('java.util.ArrayList<:[v0]>', 'java.util.HashSet<:[v0]>'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'int'),
    ('java.lang.Class', 'java.lang.reflect.Type'),




    ('org.pac4j.core.context.WebContext', 'org.pac4j.core.context.J2EContext'),
    ('java.net.URL', 'java.io.File'),
    ('android.net.Uri', 'java.lang.String'),
    ('java.util.function.Supplier<:[v0]>', 'java.util.concurrent.Callable<:[v0]>'),
    ('java.util.concurrent.ScheduledExecutorService', 'java.util.concurrent.ExecutorService'),
    ('java.nio.ByteBuffer', 'long'),
    (':[v0]', 'org.springframework.http.ResponseEntity<:[v0]>'),
    ('java.io.InputStream', 'java.io.Reader'),
    ('junit.framework.AssertionFailedError', 'java.lang.AssertionError'),
    ('java.lang.String', 'org.elasticsearch.cluster.node.DiscoveryNode'),
    ('android.content.res.Resources', 'android.content.Context'),
    ('org.elasticsearch.index.shard.ShardId', 'int'),
    ('java.sql.Statement', 'java.sql.PreparedStatement'),
    ('java.lang.String', 'java.lang.reflect.Field'),


    ('com.google.common.base.Optional<java.lang.Boolean>', 'boolean'),
    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.cache.Cache<:[v1],:[v0]>'),
    ('java.lang.Class<:[v0]>', ':[v0]'),
    ('java.util.HashMap<:[v1],:[v0]>', 'java.util.LinkedHashMap<:[v1],:[v0]>'),
    ('java.util.Map.Entry<java.lang.String,:[v0]>', ':[v0]'),
    ('java.util.concurrent.CopyOnWriteArrayList<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('int', 'java.nio.ByteBuffer'),
    ('java.lang.String', 'java.io.Serializable'),
    ('long', 'java.util.List<java.lang.Long>'),
    ('java.io.File', 'org.apache.hadoop.fs.Path'),
    ('java.lang.String', 'org.apache.hadoop.yarn.api.records.ApplicationId'),
    ('java.util.List', 'java.util.Collection'),
    ('java.util.ArrayList', 'java.util.List<java.lang.String>'),


    ('com.google.common.collect.Ordering<:[v0]>', 'java.util.Comparator<:[v0]>'),

    ('MethodDescription.InDefinedShape', 'net.bytebuddy.description.method.MethodDescription'),
    ('java.lang.Integer', 'java.lang.Double'),
    ('java.lang.StringBuilder', 'java.io.StringWriter'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.net.URI>'),
    ('java.lang.Class<T>', 'java.lang.Class<E>'),
    ('java.io.ByteArrayOutputStream', 'java.io.OutputStream'),
    ('com.google.protobuf.ByteString', 'java.lang.String'),
    ('java.time.Duration', 'long'),
    ('java.lang.Byte', 'byte'),
    ('ImmutableList.Builder<:[v0]>', 'ImmutableMap.Builder<java.lang.String,:[v0]>'),
    ('java.util.TreeMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.SortedMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.Iterator<:[v0]>', 'java.util.stream.Stream<:[v0]>'),
    ('org.pac4j.core.profile.CommonProfile', 'org.pac4j.core.profile.UserProfile'),
    ('java.lang.Long', 'java.lang.Number'),
    ('java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>', 'java.util.HashMap<:[v1],:[v0]>'),
    ('java.lang.ThreadLocal<:[v0]>', 'io.netty.util.concurrent.FastThreadLocal<:[v0]>'),
    ('io.netty.channel.nio.NioEventLoopGroup', 'io.netty.channel.EventLoopGroup'),
    ('java.lang.ClassLoader', 'java.net.URLClassLoader'),
    ('java.lang.Double', 'java.lang.String'),


    (':[v0]', 'io.netty.util.concurrent.Future<:[v0]>'),
    ('java.util.concurrent.CompletableFuture<:[v0]>', 'java.util.concurrent.Future<:[v0]>'),
    ('java.nio.charset.Charset', 'java.lang.String'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', ':[v0]'),

    ('java.util.concurrent.CompletableFuture<java.lang.Boolean>', 'java.util.concurrent.CompletableFuture<java.lang.Void>'),

    ('java.util.function.Function<:[v0],java.lang.Boolean>', 'java.util.function.Predicate<:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.NavigableSet<:[v0]>'),
    ('org.joda.time.DateTimeZone', 'java.time.ZoneId'),
    ('com.google.inject.Provider<:[v0]>', ':[v0]'),
    ('Builder<T>', 'Builder'),
    ('java.lang.String', 'java.lang.ProcessBuilder.Redirect.Type'),

    ('java.util.Map<java.lang.Integer,:[v0]>', 'java.util.Map<java.lang.Long,:[v0]>'),
    ('com.google.devtools.build.lib.vfs.Path', 'java.lang.String'),
    ('int', 'java.util.List<java.lang.String>'),
    ('org.apache.lucene.search.BooleanQuery', 'BooleanQuery.Builder'),
    ('org.apache.hadoop.fs.Path', 'java.lang.String'),
    ('java.util.Map<java.lang.Integer,:[v0]>', 'java.util.List<:[v0]>'),
    ('java.security.SecureRandom', 'java.util.Random'),

    ('java.lang.String', 'org.w3c.dom.Document'),




    ('char', 'boolean'),

    ('java.util.Optional<java.lang.Integer>', 'int'),
    ('java.lang.AssertionError', 'java.lang.Throwable'),
    ('java.util.concurrent.Future<:[v0]>', 'com.google.common.util.concurrent.ListenableFuture<:[v0]>'),

    ('com.amazonaws.services.s3.AmazonS3Client', 'com.amazonaws.services.s3.AmazonS3'),

    ('java.util.Deque<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.Map<java.lang.String,java.lang.String>', 'java.util.Map'),
    (':[v0]', 'java.util.function.Function<java.lang.String,:[v0]>'),
    ('org.elasticsearch.common.inject.Provider<:[v0]>', ':[v0]'),

    ('com.google.common.collect.ImmutableList<:[v0]>', 'com.google.common.collect.ImmutableMap<java.lang.String,:[v0]>'),
    ('java.util.concurrent.TimeUnit', 'java.lang.String'),

    ('javax.lang.model.type.DeclaredType', 'javax.lang.model.type.TypeMirror'),

    ('java.util.HashMap', 'java.util.Map'),

    ('com.google.common.collect.ImmutableCollection<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.util.Date', 'org.joda.time.DateTime'),

    ('java.util.function.Supplier<java.lang.Integer>', 'java.util.function.IntSupplier'),
    ('boolean', 'java.lang.ProcessBuilder.Redirect.Type'),
    (':[v0]', 'java.util.Map.Entry<java.lang.String,:[v0]>'),
    ('ImmutableMap.Builder<java.lang.String,:[v0]>', 'ImmutableList.Builder<:[v0]>'),
    ('javax.net.ssl.SSLContext', 'io.netty.handler.ssl.SslContext'),
    ('com.google.common.collect.ImmutableMap<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    ('java.lang.String', 'org.openqa.selenium.By'),
    ('long', 'java.util.Date'),
    ('org.reactivestreams.Publisher<:[v0]>', 'reactor.core.publisher.Mono<:[v0]>'),
    ('long', 'java.util.function.LongSupplier'),
    ('java.lang.String', 'short'),
    ('java.util.Queue<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('long', 'java.util.concurrent.atomic.AtomicInteger'),
    ('java.util.List<java.util.Map>', 'java.util.List<Map<String,Object>>'),
    ('java.lang.Comparable', 'int'),
    ('java.lang.Comparable', 'long'),
    ('double', 'java.lang.Comparable'),

    ('java.util.ArrayDeque<:[v0]>', 'java.util.Deque<:[v0]>'),
    ('rx.Observable<:[v0]>', 'io.reactivex.Flowable<:[v0]>'),
    ('java.util.stream.Stream<:[v0]>', 'java.util.Iterator<:[v0]>'),
    ('java.util.concurrent.locks.Lock', 'java.util.concurrent.locks.ReadWriteLock'),
    ('java.util.Optional<:[v0]>', 'java.util.stream.Stream<:[v0]>'),

    ('com.google.common.collect.ImmutableMap<:[v1],:[v0]>', 'com.google.common.collect.ImmutableSortedMap<:[v1],:[v0]>'),
    ('org.apache.lucene.util.BytesRef', 'java.lang.String'),
    ('com.google.common.collect.ImmutableList<:[v0]>', 'java.util.stream.Stream<:[v0]>'),
    ('Request', 'Request<T>'),

    ('double', 'java.lang.Number'),
    ('java.util.Date', 'java.lang.Long'),
    ('com.mongodb.DB', 'com.mongodb.client.MongoDatabase'),
    ('java.util.HashSet<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('org.reactivestreams.Publisher<:[v0]>', 'io.reactivex.Flowable<:[v0]>'),

    ('int', 'Result'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.lang.Long>'),
    ('java.lang.Boolean', 'java.lang.Integer'),
    (':[v0]', 'android.util.Pair<:[v0],java.lang.String>'),
    ('java.util.concurrent.TimeUnit', 'java.time.temporal.ChronoUnit'),
    ('java.net.InetSocketAddress', 'java.lang.String'),
    ('java.util.List<:[v0]>', 'java.util.Map<java.lang.Long,:[v0]>'),
    ('org.json.JSONObject', 'com.fasterxml.jackson.databind.JsonNode'),
    ('java.util.LinkedList<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('java.util.Map<:[v0],List<String>>', 'java.util.Map<:[v0],Set<String>>'),

    ('java.util.function.Function<:[v0],java.lang.Integer>', 'java.util.function.ToIntFunction<:[v0]>'),
    ('org.joda.time.DateTime', 'org.joda.time.LocalDate'),
    ('android.widget.Button', 'android.view.View'),
    ('java.util.List<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>'),
    ('android.content.pm.ApplicationInfo', 'java.lang.String'),

    ('com.google.common.util.concurrent.ListenableFuture<:[v0]>', ':[v0]'),
    ('java.util.Optional<java.nio.file.Path>', 'java.util.Optional<java.lang.String>'),
    ('int', 'android.content.res.ColorStateList'),

    ('java.lang.Class<T>', 'java.lang.Class<A>'),
    ('long', 'java.lang.Integer'),
    ('rx.Observable<:[v0]>', ':[v0]'),

    (':[v0]', 'java.util.concurrent.Future<:[v0]>'),
    ('java.lang.reflect.Method', 'java.lang.String'),

    ('java.util.SortedSet<:[v0]>', 'com.google.common.collect.ImmutableSortedSet<:[v0]>'),
    ('reactor.core.publisher.Mono<:[v0]>', 'reactor.core.publisher.Flux<:[v0]>'),
    ('java.util.List<java.net.URI>', 'java.util.List<java.lang.String>'),


    ('java.util.stream.Stream<:[v0]>', 'java.util.Set<:[v0]>'),

    ('char', 'byte'),
    ('org.junit.rules.TemporaryFolder', 'java.nio.file.Path'),



    ('ImmutableSet.Builder<:[v0]>', 'java.util.Set<:[v0]>'),

    ('java.util.concurrent.locks.ReadWriteLock', 'java.util.concurrent.locks.ReentrantReadWriteLock'),
    ('java.util.Set<:[v0]>', 'java.util.EnumSet<:[v0]>'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'java.util.concurrent.atomic.AtomicLong'),
    ('com.google.common.collect.ImmutableSortedSet<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('com.google.common.util.concurrent.ListeningExecutorService', 'java.util.concurrent.ExecutorService'),



    ('javax.sql.DataSource', 'org.springframework.jdbc.core.JdbcTemplate'),
    ('java.util.Queue<:[v0]>', 'java.util.Set<:[v0]>'),
    ('org.springframework.core.io.Resource', 'java.lang.String'),
    ('zipkin2.Span', 'brave.handler.MutableSpan'),
    ('java.util.List<:[v0]>', 'java.util.TreeSet<:[v0]>'),
    ('com.google.common.truth.SubjectFactory<:[v1],:[v0]>', 'Subject.Factory<:[v1],:[v0]>'),
    ('com.google.common.truth.FailureStrategy', 'com.google.common.truth.FailureMetadata'),
    ('android.widget.ImageButton', 'android.view.View'),
    ('java.util.List<java.net.URL>', 'java.util.List<java.net.URI>'),


    ('int', 'com.google.common.base.Optional<java.lang.Integer>'),

    ('java.util.List<:[v0]>', 'java.util.Map<:[6b8ba2a6-516a-3947-39a9-adc89f0ffbda_v0_equal],:[6b8ba2a6-516a-3947-39a9-adc89f0ffbda_v0_equal]>'),
    ('java.util.concurrent.Executor', 'java.util.concurrent.ScheduledExecutorService'),


    ('com.sun.tools.javac.tree.JCTree', 'com.sun.source.tree.Tree'),
    ('java.io.FileOutputStream', 'java.io.BufferedOutputStream'),
    ('java.util.ArrayList', 'java.util.ArrayList<java.lang.String>'),
    ('java.util.concurrent.Future<:[v0]>', ':[v0]'),
    ('org.elasticsearch.search.SearchHitField', 'org.elasticsearch.common.document.DocumentField'),
    ('java.io.PrintStream', 'java.io.PrintWriter'),
    ('int', 'java.util.Collection<java.lang.Integer>'),
    ('javax.lang.model.type.TypeMirror', 'javax.lang.model.element.TypeElement'),

    ('java.util.Queue<:[v0]>', 'java.util.ArrayDeque<:[v0]>'),

    ('java.util.function.Function<:[v0],:[v0]>', 'java.util.function.UnaryOperator<:[v0]>'),
    ('User', 'java.lang.String'),

    ('java.util.concurrent.ForkJoinPool', 'java.util.concurrent.ExecutorService'),
    ('android.widget.Button', 'android.widget.TextView'),

    ('org.elasticsearch.common.collect.ImmutableOpenMap<java.lang.String,:[v0]>', ':[v0]'),
    ('java.util.SortedSet<:[v0]>', 'java.util.List<:[v0]>'),
    ('org.elasticsearch.Version', 'java.lang.String'),
    ('boolean', 'java.lang.Runnable'),
    ('java.util.concurrent.Executor', 'com.google.common.util.concurrent.ListeningExecutorService'),
    ('com.google.common.base.Optional<java.lang.Integer>', 'int'),

    ('java.util.List<java.lang.Integer>', 'java.util.List<T>'),
    ('java.io.InputStream', 'java.net.URL'),
    ('javax.servlet.ServletResponse', 'javax.servlet.http.HttpServletResponse'),
    ('boolean', 'Result'),


    ('com.google.common.util.concurrent.ListenableFuture<:[v0]>', 'java.util.concurrent.Future<:[v0]>'),

    ('android.view.View', 'android.widget.ImageView'),


    ('org.elasticsearch.env.Environment', 'java.nio.file.Path'),
    ('com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient', 'com.amazonaws.services.dynamodbv2.AmazonDynamoDB'),
    ('java.lang.Long', 'java.time.Duration'),
    ('java.lang.Integer', 'boolean'),
    ('java.util.Properties', 'java.util.Map'),
    ('java.util.concurrent.CompletableFuture<java.lang.Void>', 'java.util.concurrent.CompletableFuture<java.lang.Long>'),
    (':[v0]', 'java.util.concurrent.Callable<:[v0]>'),


    ('java.util.concurrent.atomic.AtomicInteger', 'org.apache.commons.lang3.mutable.MutableInt'),
    ('java.lang.String', 'android.content.Intent'),
    ('java.util.List<java.lang.Integer>', 'com.google.protobuf.Internal.IntList'),
    ('org.apache.http.impl.client.DefaultHttpClient', 'org.apache.http.impl.client.CloseableHttpClient'),
    ('ImmutableList.Builder<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),
    ('java.util.concurrent.Callable<:[v0]>', ':[v0]'),
    ('int', 'java.net.InetSocketAddress'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.lang.Iterable<:[v0]>'),
    ('java.io.InputStream', 'java.io.BufferedInputStream'),
    ('java.util.Map<java.net.URI,:[v0]>', 'java.util.Map<java.net.URL,:[v0]>'),

    ('java.util.Set<java.lang.String>', 'java.util.Set<java.nio.file.Path>'),
    ('java.util.Set<:[v0]>', 'com.google.common.collect.ImmutableSortedSet<:[v0]>'),
    ('long', 'java.util.OptionalLong'),
    ('java.util.Iterator<:[v0]>', ':[v0]'),

    ('java.util.UUID', 'long'),
    ('javafx.beans.property.ObjectProperty<:[v0]>', ':[v0]'),
    ('boolean', 'java.util.concurrent.CompletableFuture<java.lang.Void>'),
    ('org.apache.hadoop.conf.Configuration', 'org.apache.hadoop.yarn.conf.YarnConfiguration'),
    ('org.apache.hadoop.hbase.client.HTable', 'org.apache.hadoop.hbase.client.Table'),
    ('org.apache.hadoop.hbase.client.HTableInterface', 'org.apache.hadoop.hbase.client.Table'),
    ('java.lang.String', 'java.util.Properties'),

    ('java.lang.String', 'java.lang.ProcessEnvironment.Variable'),

    ('Map.Entry<:[v1],:[v0]>', 'Map.Entry<:[v0],:[v1]>'),
    ('java.util.Properties', 'java.lang.String'),
    (':[v0]', 'java.util.concurrent.CompletionStage<:[v0]>'),
    ('java.time.ZonedDateTime', 'java.time.Instant'),
    ('Resource', 'java.lang.String'),

    ('java.lang.String', 'org.elasticsearch.common.settings.Setting<java.lang.Long>'),
    ('int', 'org.apache.http.HttpResponse'),

    (':[v0]', 'com.google.common.collect.ImmutableMap<java.lang.String,:[v0]>'),
    ('android.support.v7.widget.LinearLayoutManager', 'android.support.v7.widget.GridLayoutManager'),
    ('java.util.ArrayList<:[v0]>', 'java.util.Map<java.lang.String,:[v0]>'),
    ('org.elasticsearch.common.transport.InetSocketTransportAddress', 'org.elasticsearch.common.transport.TransportAddress'),
    ('brave.Tracer', 'brave.Tracing'),
    (':[v0]', 'java.util.Map.Entry<:[v0],:[v0]>'),
    ('org.json.JSONArray', 'com.fasterxml.jackson.databind.JsonNode'),
    ('com.mongodb.DBObject', 'org.bson.Document'),
    ('java.io.BufferedOutputStream', 'java.io.OutputStream'),
    ('java.util.Set<:[v0]>', 'ImmutableSet.Builder<:[v0]>'),

    ('int', 'java.lang.Thread.State'),
    ('java.nio.file.Path', 'java.net.URI'),
    ('java.lang.Runnable', 'java.util.function.Consumer<java.lang.String>'),
    ('java.util.SortedSet<:[v0]>', 'java.util.NavigableSet<:[v0]>'),
    ('java.util.List<java.lang.String>', 'int'),
    ('java.io.File', 'android.content.Context'),

    ('java.lang.String', 'Status'),
    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.collect.BiMap<:[v1],:[v0]>'),
    ('float', 'java.lang.String'),
    ('java.io.OutputStream', 'java.io.ByteArrayOutputStream'),
    ('java.util.concurrent.ThreadPoolExecutor', 'java.util.concurrent.Executor'),

    ('java.lang.String', 'com.google.protobuf.ByteString'),

    ('boolean', 'java.lang.Long'),
    ('java.util.List<java.io.File>', 'java.util.List<java.lang.String>'),
    ('java.util.Stack<:[v0]>', 'java.util.ArrayDeque<:[v0]>'),
    ('com.google.common.collect.ImmutableCollection<:[v0]>', 'java.lang.Iterable<:[v0]>'),

    ('java.util.concurrent.Callable<:[v0]>', 'java.util.function.Supplier<:[v0]>'),
    ('java.util.LinkedHashSet<:[v0]>', 'java.util.Collection<:[v0]>'),
    ('java.util.Map<:[v0],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('android.graphics.drawable.Drawable', 'int'),
    ('java.lang.Long', 'java.time.Instant'),
    ('net.bytebuddy.description.type.TypeDefinition', 'net.bytebuddy.description.type.TypeDescription'),
    ('java.lang.String', 'java.lang.Double'),
    ('org.apache.hadoop.fs.Path', 'org.apache.hadoop.fs.FileStatus'),
    ('android.graphics.Rect', 'android.graphics.RectF'),
    ('java.net.URL', 'android.net.Uri'),
    ('java.net.Socket', 'javax.net.ssl.SSLSocket'),

    ('android.widget.ImageView', 'android.view.View'),
    ('java.util.Collection<:[v0]>', 'com.google.common.collect.ImmutableCollection<:[v0]>'),

    ('java.util.Optional<java.lang.String>', 'java.util.Optional<java.nio.file.Path>'),
    ('java.util.Optional<java.lang.Long>', 'java.util.OptionalLong'),
    ('java.io.InputStream', 'java.io.FileInputStream'),

    ('java.time.ZonedDateTime', 'java.time.LocalDate'),
    ('java.util.List<:[v0]>', 'java.util.LinkedHashMap<java.lang.String,:[v0]>'),
    (':[v0]', 'org.hamcrest.Matcher<:[v0]>'),
    ('java.lang.String', 'java.lang.reflect.Method'),
    ('java.util.ArrayList', 'java.util.ArrayList<java.lang.Integer>'),
    ('org.elasticsearch.client.transport.TransportClient', 'org.elasticsearch.client.RestHighLevelClient'),

    ('java.util.HashMap<:[v1],:[v0]>', 'java.util.TreeMap<:[v1],:[v0]>'),
    ('javax.lang.model.element.Element', 'javax.lang.model.element.TypeElement'),
    ('java.util.TreeMap<:[v1],:[v0]>', 'java.util.LinkedHashMap<:[v1],:[v0]>'),
    ('java.math.BigInteger', 'long'),
    ('org.elasticsearch.action.support.PlainActionFuture<:[v0]>', 'org.elasticsearch.action.ActionListener<:[v0]>'),
    ('java.util.TreeSet<:[v0]>', 'java.util.List<:[v0]>'),


    ('java.util.function.Supplier<java.lang.Boolean>', 'java.util.function.BooleanSupplier'),
    ('java.util.function.Predicate<java.lang.Long>', 'java.util.function.LongPredicate'),
    ('java.util.Map<:[v0],java.lang.Boolean>', 'java.util.Set<:[v0]>'),
    ('java.util.concurrent.ConcurrentHashMap<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    (':[v0]', 'java.util.function.Predicate<:[v0]>'),
    ('org.elasticsearch.cluster.ClusterChangedEvent', 'org.elasticsearch.cluster.ClusterState'),
    ('java.util.ArrayList<java.lang.Integer>', 'java.util.ArrayList<java.lang.String>'),
    ('java.util.Calendar', 'java.time.ZonedDateTime'),
    ('long', 'short'),
    ('byte', 'long'),
    ('java.util.Collection<:[v0]>', 'java.util.Iterator<:[v0]>'),
    ('java.util.Date', 'java.time.LocalDateTime'),
    ('com.google.common.collect.ImmutableMap<:[v0],:[v0]>', 'java.util.Map<:[v0],:[v0]>'),
    ('java.util.List<:[v0]>', 'java.util.concurrent.ConcurrentLinkedDeque<:[v0]>'),
    ('io.netty.channel.socket.SocketChannel', 'io.netty.channel.Channel'),
    ('java.util.Enumeration<:[v0]>', 'java.util.List<:[v0]>'),

    ('java.io.InputStream', 'java.lang.String'),
    (':[v0]', 'Map.Entry<:[v0],java.lang.Double>'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<K>'),
    ('java.util.Map<:[v1],:[v1]>', 'java.util.Set<:[v1]>'),
    (':[v0]', 'com.google.common.collect.ImmutableSortedSet<:[v0]>'),


    ('java.lang.String', 'org.apache.lucene.util.BytesRef'),
    ('java.util.Set<java.lang.Long>', 'java.util.Set<java.lang.String>'),




    ('java.lang.ProcessBuilder.Redirect.Type', 'java.lang.String'),

    ('retrofit2.Call<:[v0]>', 'rx.Observable<:[v0]>'),
    ('android.graphics.Paint', 'android.text.TextPaint'),
    ('java.util.LinkedList<:[v0]>', 'java.util.Queue<:[v0]>'),

    ('java.util.List<java.lang.Long>', 'java.util.List<java.lang.Integer>'),
    ('double', 'java.math.BigDecimal'),
    ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.AtomicBoolean'),

    ('java.util.Iterator', 'java.util.Iterator<T>'),
    ('java.util.Set<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('short', 'long'),


    ('java.text.SimpleDateFormat', 'java.time.format.DateTimeFormatter'),
    ('java.util.List<:[v0]>', 'java.util.LinkedHashSet<:[v0]>'),
    ('java.lang.Number', 'java.lang.Long'),

    ('javax.lang.model.element.TypeElement', 'javax.lang.model.element.Element'),
    ('java.util.concurrent.Future<:[v0]>', 'java.util.concurrent.ScheduledFuture<:[v0]>'),
    ('short', 'double'),
    ('java.util.List<java.lang.String>', 'java.util.List'),

    ('java.lang.String', 'javax.ws.rs.core.Response'),

    ('java.lang.String', 'java.util.concurrent.ExecutorService'),
    ('org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('java.util.Optional<java.lang.Long>', 'long'),

    ('byte', 'java.lang.String'),

    ('java.lang.InheritableThreadLocal<:[v0]>', 'java.lang.ThreadLocal<:[v0]>'),











    ('java.util.concurrent.SynchronousQueue<:[v0]>', 'java.util.concurrent.LinkedBlockingQueue<:[v0]>'),
    ('com.google.common.cache.Cache<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),
    ('java.util.concurrent.ScheduledFuture<:[v0]>', 'java.util.concurrent.Future<:[v0]>'),
    ('java.io.DataInputStream', 'java.io.DataInput'),
    ('java.util.concurrent.locks.Condition', 'java.util.concurrent.CountDownLatch'),
    ('javax.servlet.http.ServletMapping', 'javax.servlet.http.HttpServletMapping'),

    ('java.lang.StringBuffer', 'java.lang.String'),
    ('org.elasticsearch.cluster.ClusterState', 'org.elasticsearch.cluster.metadata.MetaData'),
    ('android.widget.TextView', 'android.widget.Button'),
    ('org.pac4j.http.profile.HttpProfile', 'org.pac4j.core.profile.CommonProfile'),
    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.collect.ImmutableSortedMap<:[v1],:[v0]>'),
    ('int', 'java.sql.JDBCType'),
    ('android.view.ViewGroup', 'android.view.View'),
    ('java.util.Iterator<java.lang.String>', 'java.util.Iterator'),
    (':[v0]', 'net.minecraft.util.ActionResult<:[v0]>'),
    ('java.nio.ByteBuffer', 'io.netty.buffer.ByteBuf'),
    ('java.util.concurrent.atomic.AtomicInteger', 'java.util.concurrent.atomic.AtomicBoolean'),

    ('org.elasticsearch.index.analysis.AnalysisService', 'org.elasticsearch.index.analysis.IndexAnalyzers'),
    ('android.app.Notification', 'Notification.Builder'),
    ('ReplicationOperation.Replicas', 'ReplicationOperation.Replicas<ReplicaRequest>'),
    ('rx.Observable<:[v0]>', 'rx.Single<:[v0]>'),
    ('org.elasticsearch.common.settings.Settings', 'int'),
    ('javax.servlet.ServletRequest', 'javax.servlet.http.HttpServletRequest'),

    ('java.util.Optional<java.lang.Integer>', 'java.util.Optional<java.lang.Long>'),
    ('java.util.List<org.apache.hadoop.hbase.KeyValue>', 'java.util.List<org.apache.hadoop.hbase.Cell>'),
    ('android.widget.LinearLayout', 'android.widget.TextView'),
    ('java.util.List<Map<String,String>>', 'java.util.List<Map<String,Object>>'),
    ('com.google.common.cache.Cache<:[v1],:[v0]>', 'com.google.common.cache.LoadingCache<:[v1],:[v0]>'),
    ('org.hibernate.SQLQuery', 'org.hibernate.query.NativeQuery'),
    ('java.lang.Thread.State', 'int'),
    (':[v0]', 'java.lang.Class<:[v0]>'),

    ('java.io.Reader', 'java.io.InputStream'),
    ('AsyncShardFetch.FetchResult<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards>', 'AsyncShardFetch.FetchResult<org.elasticsearch.gateway.TransportNodesListGatewayStartedShards.NodeGatewayStartedShards>'),
    (':[v0]', 'java.util.EnumSet<:[v0]>'),
    ('org.hamcrest.Matcher<:[v0]>', 'org.mockito.ArgumentMatcher<:[v0]>'),
    (':[v0]', 'java.util.function.Consumer<:[v0]>'),
    ('java.lang.String', 'java.time.Instant'),
    ('java.lang.String', 'java.util.Map'),

    ('java.io.BufferedReader', 'java.io.InputStream'),
    ('com.google.common.collect.BiMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('long', 'com.google.common.base.Optional<java.lang.Long>'),
    ('java.io.RandomAccessFile', 'java.io.File'),
    ('android.content.Intent', 'int'),
    ('java.lang.String', 'org.apache.logging.log4j.Level'),
    ('java.io.File', 'boolean'),

    ('org.jets3t.service.S3Service', 'com.amazonaws.services.s3.AmazonS3'),
    ('java.util.List<:[v0]>', 'com.google.common.collect.ImmutableSet<:[v0]>'),
    ('java.lang.Long', 'java.math.BigInteger'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<java.lang.Long>'),



    ('java.util.Dictionary', 'java.util.Dictionary<java.lang.String,java.lang.String>'),
    ('java.util.stream.Stream<:[v0]>', 'java.util.Optional<:[v0]>'),

    ('java.lang.Class<:[v0]>', 'ChannelFactory<:[v0]>'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.net.InetAddress>'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<Map.Entry<String,Object>>'),

    ('org.jboss.as.controller.SimpleAttributeDefinition', 'org.jboss.as.controller.AttributeDefinition'),
    ('java.util.PriorityQueue<:[v0]>', 'java.util.NavigableSet<:[v0]>'),
    ('com.squareup.javapoet.TypeName', 'com.squareup.javapoet.ClassName'),
    ('com.github.javaparser.ast.Modifier', 'com.github.javaparser.ast.Modifier.Keyword'),
    ('org.apache.lucene.search.Scorer', 'org.apache.lucene.search.Scorable'),
    ('boolean', 'org.apache.lucene.search.ScoreMode'),
    ('int', 'org.graylog.plugins.sidecar.mapper.SidecarStatusMapper.Status'),
    ('dalvik.system.PathClassLoader', 'java.lang.ClassLoader'),
    ('org.jboss.resteasy.client.jaxrs.ResteasyClient', 'javax.ws.rs.client.Client'),
    ('byte', 'double'),
    ('javax.lang.model.element.TypeElement', 'javax.lang.model.type.TypeMirror'),
    ('javax.lang.model.element.TypeElement', 'javax.lang.model.type.DeclaredType'),

    ('org.elasticsearch.common.collect.CopyOnWriteHashMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('org.apache.commons.httpclient.HttpMethod', 'org.apache.http.HttpResponse'),

    ('boolean', 'java.lang.Integer'),

    ('java.lang.Void', 'java.lang.Boolean'),
    ('long', 'java.time.ZonedDateTime'),

    ('java.lang.ClassValue.Version', 'java.lang.String'),
    ('java.lang.Class<T>', 'java.lang.String'),
    ('java.util.EnumSet<:[v0]>', ':[v0]'),
    ('org.elasticsearch.common.io.stream.BytesStreamOutput', 'java.io.ByteArrayOutputStream'),
    ('android.content.Intent', 'android.os.Bundle'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.Map<java.lang.Long,:[v0]>'),
    ('org.eclipse.jetty.webapp.WebAppContext', 'org.eclipse.jetty.servlet.ServletContextHandler'),

    ('java.lang.StringBuilder', 'java.lang.Appendable'),
    ('javax.servlet.http.HttpServletRequest', 'java.lang.String'),
    ('com.mongodb.DBCollection', 'com.mongodb.client.MongoCollection<org.bson.Document>'),

    ('java.util.List', 'java.lang.String'),
    ('java.lang.Class', 'java.lang.Class<java.lang.String>'),
    ('java.util.Map<java.lang.Long,:[v0]>', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.util.HashMap<java.lang.String,:[v0]>', ':[v0]'),
    ('java.lang.Class<T>', 'java.lang.Class<U>'),
    ('javax.servlet.ServletContext', 'java.lang.String'),
    ('GroupShardsIterator', 'GroupShardsIterator<ShardIterator>'),

    ('float', 'boolean'),
    ('char', 'short'),
    ('java.util.concurrent.CopyOnWriteArrayList<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.time.Instant', 'long'),
    ('java.util.Vector<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('java.util.Map<java.lang.Integer,:[v0]>', ':[v0]'),
    ('org.apache.spark.sql.DataFrame', 'org.apache.spark.sql.Dataset<org.apache.spark.sql.Row>'),
    ('java.util.List<:[v0]>', 'java.util.Map<:[v0],java.lang.Long>'),

    ('android.util.SparseArray<:[v0]>', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.util.Collection<:[v0]>', 'java.util.Map<java.lang.Integer,:[v0]>'),
    ('java.util.Queue<:[v0]>', ':[v0]'),
    ('java.lang.String', 'Identifier'),

    ('java.lang.Throwable', 'boolean'),
    ('net.minecraft.block.Block', 'net.minecraft.block.state.IBlockState'),
    ('com.datastax.driver.core.Session', 'com.datastax.oss.driver.api.core.CqlSession'),
    ('java.util.concurrent.Executor', 'java.lang.String'),
    ('java.util.ArrayList<:[v0]>', 'java.util.concurrent.CopyOnWriteArrayList<:[v0]>'),

    ('java.util.Collection<List<String>>', 'java.util.Collection<java.util.Collection<String>>'),
    ('boolean', 'java.util.Set<java.lang.Integer>'),
    ('java.io.InputStream', 'java.io.OutputStream'),
    ('boolean', 'short'),
    ('boolean', 'char'),
    ('java.util.concurrent.ExecutorService', 'int'),


    ('java.util.Deque<:[v0]>', 'java.util.LinkedList<:[v0]>'),
    ('javax.lang.model.type.DeclaredType', 'javax.lang.model.element.TypeElement'),

    ('java.util.Enumeration', 'java.util.Enumeration<java.util.Locale>'),
    ('org.springframework.http.ResponseEntity', 'org.springframework.http.ResponseEntity<java.lang.String>'),
    ('java.util.concurrent.Semaphore', 'java.util.concurrent.atomic.AtomicInteger'),
    ('char', 'double'),
    ('com.google.common.base.Optional<:[v0]>', 'com.google.common.collect.ImmutableList<:[v0]>'),


    ('io.netty.handler.codec.http.DefaultFullHttpResponse', 'io.netty.handler.codec.http.FullHttpResponse'),
    ('java.util.concurrent.CompletableFuture<java.lang.Boolean>', 'boolean'),
    ('char', 'java.lang.Character'),

    ('java.io.OutputStream', 'java.io.BufferedOutputStream'),

    ('java.util.List<:[v0]>', 'java.util.Map<:[v0],:[v0]>'),
    ('java.lang.Number', 'int'),
    ('java.util.List<:[v0]>', 'com.github.javaparser.ast.NodeList<:[v0]>'),

    ('java.lang.String', 'java.lang.ClassLoader'),
    (':[v0]', 'javafx.collections.ObservableList<:[v0]>'),
    ('java.lang.reflect.AnnotatedElement', 'java.lang.reflect.Method'),
    ('com.sun.jersey.spi.container.ContainerRequest', 'javax.ws.rs.container.ContainerRequestContext'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'ImmutableSet.Builder<:[v0]>'),
    ('android.graphics.drawable.ColorDrawable', 'android.graphics.drawable.Drawable'),
    ('java.io.FileWriter', 'java.io.PrintWriter'),
    ('java.nio.file.Path', 'org.neo4j.kernel.configuration.Config'),
    ('java.io.Serializable', 'java.lang.String'),
    ('java.lang.String', 'org.apache.flink.configuration.Configuration'),
    ('org.elasticsearch.common.text.Text', 'java.lang.String'),
    ('boolean', 'net.minecraft.util.EnumActionResult'),
    ('android.widget.ListView', 'androidx.recyclerview.widget.RecyclerView'),
    ('java.util.LinkedHashMap<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    ('java.util.Map<org.mortbay.jetty.servlet.Context,:[v0]>', 'java.util.Map<org.eclipse.jetty.servlet.ServletContextHandler,:[v0]>'),
    ('org.mortbay.jetty.servlet.Context', 'org.eclipse.jetty.servlet.ServletContextHandler'),
    ('Map.Entry<org.mortbay.jetty.servlet.Context,:[v0]>', 'Map.Entry<org.eclipse.jetty.servlet.ServletContextHandler,:[v0]>'),
    ('org.mortbay.jetty.Connector', 'org.eclipse.jetty.server.ServerConnector'),
    ('org.elasticsearch.cluster.node.DiscoveryNodes', 'java.util.Collection<org.elasticsearch.cluster.node.DiscoveryNode>'),

    ('boolean', 'com.google.common.base.Optional<java.lang.Boolean>'),

    ('java.util.Enumeration', 'java.util.Enumeration<java.net.InetAddress>'),
    ('java.util.Enumeration', 'java.util.Enumeration<java.net.NetworkInterface>'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.IdentityHashMap<:[v1],:[v0]>'),
    ('Map.Entry<java.lang.Long,:[v0]>', ':[v0]'),

    ('java.util.Timer', 'java.util.concurrent.ScheduledExecutorService'),


    ('java.util.Map<java.lang.Long,:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.UUID', 'int'),
    ('io.netty.channel.EventLoop', 'io.netty.util.concurrent.EventExecutor'),
    ('java.lang.Double', 'int'),
    ('java.lang.StringBuilder', 'java.io.ByteArrayOutputStream'),
    ('org.elasticsearch.action.ListenableActionFuture<:[v0]>', 'org.elasticsearch.action.ActionFuture<:[v0]>'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<T>'),
    ('java.util.Map<:[v0],:[v1]>', 'java.util.concurrent.ConcurrentMap<:[v0],:[v1]>'),
    ('java.util.Set<:[v0]>', 'java.util.function.Predicate<:[v0]>'),
    ('java.lang.Void', 'java.lang.String'),
    ('java.util.concurrent.Callable<java.lang.Void>', 'java.lang.Runnable'),

    ('java.util.Collection<java.io.File>', 'java.util.Collection<java.nio.file.Path>'),
    ('java.util.Map<java.io.File,:[v0]>', 'java.util.Map<java.nio.file.Path,:[v0]>'),
    ('java.util.concurrent.CompletableFuture<:[v0]>', 'com.google.common.util.concurrent.SettableFuture<:[v0]>'),


    ('java.lang.String', 'com.google.common.hash.HashCode'),




    ('io.netty.handler.codec.http.HttpRequest', 'io.netty.handler.codec.http.FullHttpRequest'),
    ('org.hamcrest.Matcher<:[v0]>', ':[v0]'),


    ('org.apache.hadoop.hbase.client.HBaseAdmin', 'org.apache.hadoop.hbase.client.Admin'),
    ('int', 'java.lang.Throwable'),
    ('java.util.function.Function<java.lang.Long,:[v0]>', 'java.util.function.LongFunction<:[v0]>'),
    ('java.util.function.Supplier<:[v0]>', 'java.util.function.IntFunction<:[v0]>'),
    ('org.spongepowered.api.data.DataContainer', 'org.spongepowered.api.data.DataView'),

    ('java.time.ZoneOffset', 'java.time.ZoneId'),
    ('com.google.common.collect.ImmutableSet<:[v0]>', 'java.util.SortedSet<:[v0]>'),


    ('org.ehcache.CacheManager', 'EhcacheManager'),
    ('dagger.Lazy<:[v0]>', ':[v0]'),
    ('android.view.View', 'android.widget.FrameLayout'),

    ('java.util.concurrent.ConcurrentMap<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),
    ('java.util.Vector', 'java.util.Vector<java.lang.String>'),
    ('org.joda.time.DateTime', 'java.time.LocalTime'),
    ('int', 'java.util.concurrent.CompletableFuture<java.lang.Integer>'),
    ('java.util.Optional<T>', 'java.util.Optional<U>'),
    ('java.time.Clock', 'java.util.function.LongSupplier'),

    ('java.util.ServiceLoader<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.util.Map<java.lang.String,java.lang.String>', 'org.elasticsearch.env.Environment'),
    ('java.lang.Double', 'java.lang.Integer'),
    ('java.util.List<:[v0]>', 'java.util.ArrayDeque<:[v0]>'),
    ('java.lang.String', 'org.springframework.web.servlet.ModelAndView'),

    ('java.util.IdentityHashMap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    (':[v0]', 'java.util.Stack<:[v0]>'),
    ('java.util.concurrent.LinkedBlockingQueue<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>'),
    ('java.util.Collection<:[v0]>', 'java.util.HashSet<:[v0]>'),
    ('java.util.concurrent.locks.Lock', 'java.util.concurrent.locks.ReentrantLock'),
    ('java.lang.reflect.Field', 'java.lang.reflect.AccessibleObject'),


    ('androidx.recyclerview.widget.RecyclerView', 'android.view.View'),
    ('java.util.NavigableMap<java.lang.String,:[v0]>', 'java.util.NavigableMap<K,:[v0]>'),
    ('java.lang.reflect.Method', 'java.lang.reflect.Field'),

    ('android.widget.TextView', 'android.widget.ImageButton'),
    ('android.widget.LinearLayout', 'android.view.View'),
    ('long', 'java.util.List<java.lang.String>'),
    ('java.lang.Integer', 'java.lang.Float'),
    ('java.lang.Class<T>', 'java.lang.Class<V>'),
    ('java.lang.String', 'java.lang.Class<T>'),
    ('java.util.Map<java.lang.String,:[v0]>', 'java.util.Map<Optional<String>,:[v0]>'),
    ('Map.Entry<java.lang.String,:[v0]>', 'Map.Entry<Optional<String>,:[v0]>'),
    ('long', 'java.math.BigDecimal'),
    ('int', 'java.util.function.IntPredicate'),
    ('java.util.concurrent.atomic.AtomicLong', 'java.lang.Long'),
    ('scala.Option<:[v0]>', 'java.util.Optional<:[v0]>'),

    ('java.util.NavigableSet<:[v0]>', 'java.util.Set<:[v0]>'),
    ('java.lang.String', 'java.lang.Number'),
    ('java.util.ArrayList<T>', 'java.util.ArrayList<java.lang.String>'),

    ('long', 'org.elasticsearch.common.unit.ByteSizeValue'),
    ('org.elasticsearch.index.shard.ShardId', 'java.lang.String'),
    ('org.jgroups.Channel', 'org.jgroups.JChannel'),
    ('java.util.Set<java.nio.file.Path>', 'java.util.Set<java.lang.String>'),
    ('java.util.Date', 'java.time.ZonedDateTime'),






    ('java.util.concurrent.locks.ReentrantLock', 'java.util.concurrent.locks.Lock'),
    ('org.elasticsearch.common.util.BigArrays', 'org.elasticsearch.common.util.PageCacheRecycler'),
    ('java.util.concurrent.atomic.AtomicReference<:[v0]>', 'java.util.Optional<:[v0]>'),
    ('java.sql.Date', 'java.time.LocalDate'),
    ('java.sql.Timestamp', 'java.time.LocalDateTime'),
    ('java.util.Queue<:[v0]>', 'java.util.PriorityQueue<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'org.springframework.util.MultiValueMap<:[v1],:[v0]>'),
    ('java.util.concurrent.BlockingQueue<zipkin2.Span>', 'java.util.concurrent.BlockingQueue<brave.handler.MutableSpan>'),
    ('java.lang.String', 'org.apache.hadoop.conf.Configuration'),

    ('java.util.Collection<:[v0]>', 'java.util.LinkedHashSet<:[v0]>'),
    ('org.apache.flink.streaming.runtime.tasks.OperatorStateHandles', 'org.apache.flink.runtime.checkpoint.OperatorSubtaskState'),
    ('java.util.function.ToIntFunction<:[v0]>', 'java.util.function.ToLongFunction<:[v0]>'),
    ('java.util.concurrent.ScheduledThreadPoolExecutor', 'java.util.concurrent.ScheduledExecutorService'),
    ('java.util.Queue<:[v0]>', 'java.util.LinkedList<:[v0]>'),
    ('java.util.List<java.lang.Integer>', 'com.carrotsearch.hppc.IntArrayList'),
    ('javax.lang.model.element.Element', 'javax.lang.model.element.ExecutableElement'),
    ('javax.lang.model.type.TypeMirror', 'javax.lang.model.type.DeclaredType'),

    ('org.pac4j.oauth.credentials.OAuthCredentials', 'org.pac4j.oauth.credentials.OAuth20Credentials'),
    ('org.elasticsearch.cluster.ClusterState', 'long'),
    ('java.util.List<java.lang.Integer>', 'int'),
    ('java.lang.Iterable<org.elasticsearch.cluster.node.DiscoveryNode>', 'org.elasticsearch.cluster.node.DiscoveryNodes'),

    ('java.lang.Class', 'java.lang.String'),
    ('org.json.JSONObject', 'java.lang.String'),
    ('java.util.Locale', 'java.lang.String'),
    ('java.lang.String', 'org.elasticsearch.cluster.service.ClusterService'),
    ('java.util.List<Map.Entry<K,V>>', 'java.util.List<Entry<K,V>>'),
    ('java.util.concurrent.atomic.LongAdder', 'long'),
    ('java.util.List<java.lang.Throwable>', 'java.util.List<java.lang.String>'),
    ('java.util.concurrent.ConcurrentLinkedQueue<:[v0]>', 'java.util.concurrent.ConcurrentLinkedDeque<:[v0]>'),
    ('java.lang.String', 'net.minecraft.util.ResourceLocation'),

    ('java.lang.String', 'javax.servlet.http.HttpServletRequest'),
    ('java.util.function.Consumer<:[v0]>', 'java.util.function.BiConsumer<:[v0],java.lang.String>'),
    ('java.lang.String', 'org.elasticsearch.cluster.metadata.RepositoryMetaData'),
    ('java.lang.Boolean', 'java.lang.Long'),

    ('org.elasticsearch.common.bytes.BytesArray', 'org.elasticsearch.common.bytes.BytesReference'),


    ('java.util.OptionalInt', 'java.util.Optional<java.lang.Integer>'),

    ('javax.management.ObjectName', 'java.lang.String'),

    ('rx.Observable<:[v0]>', 'io.reactivex.Single<:[v0]>'),
    ('java.util.TreeMap<:[v1],:[v0]>', 'java.util.SortedMap<:[v1],:[v0]>'),
    ('java.lang.reflect.Field', 'java.lang.reflect.Member'),
    ('org.apache.hadoop.fs.FSDataOutputStream', 'java.io.OutputStream'),
    ('org.apache.catalina.connector.Request', 'javax.servlet.http.HttpServletRequest'),

    ('java.util.concurrent.atomic.AtomicBoolean', 'org.apache.commons.lang3.mutable.MutableBoolean'),
    ('java.lang.String', 'java.lang.ClassValue.Version'),
    ('java.lang.Comparable<:[v0]>', ':[v0]'),
    ('com.sun.tools.javac.main.Main.Result', 'boolean'),
    ('org.eclipse.jetty.server.Connector', 'org.eclipse.jetty.server.ServerConnector'),
    ('org.apache.hadoop.conf.Configuration', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('java.util.function.Consumer<java.lang.Long>', 'java.util.function.LongConsumer'),
    ('net.minecraft.util.MovingObjectPosition', 'net.minecraft.util.math.RayTraceResult'),
    ('net.minecraft.util.Vec3', 'net.minecraft.util.math.Vec3d'),
    (':[v0]', 'javafx.beans.property.ObjectProperty<:[v0]>'),
    ('java.util.Set<java.lang.String>', 'int'),
    ('org.pac4j.core.context.JEEContext', 'org.pac4j.core.context.WebContext'),
    ('java.time.ZoneId', 'java.util.TimeZone'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'java.util.concurrent.atomic.AtomicReference<java.lang.Thread.State>'),
    ('java.lang.Integer', 'java.lang.Boolean'),
    ('java.time.LocalDate', 'java.time.LocalDateTime'),

    ('android.widget.ListView', 'android.support.v7.widget.RecyclerView'),
    ('java.io.DataOutputStream', 'java.io.DataOutput'),
    ('java.util.concurrent.CompletableFuture<java.lang.Void>', 'java.util.concurrent.CompletableFuture<T>'),
    ('java.util.LinkedHashSet<:[v0]>', 'java.util.ArrayList<:[v0]>'),
    ('java.io.OutputStreamWriter', 'java.io.Writer'),
    ('java.io.StringWriter', 'java.lang.StringBuilder'),
    ('java.util.List<:[v0]>', 'java.util.HashMap<java.lang.String,:[v0]>'),

    ('short', 'boolean'),
    ('java.util.concurrent.TimeUnit', 'java.time.temporal.TemporalUnit'),
    ('java.nio.CharBuffer', 'java.nio.ByteBuffer'),
    ('reactor.core.publisher.MonoProcessor<:[v0]>', 'Sinks.Empty<:[v0]>'),
    ('java.util.function.Consumer<:[v0]>', 'java.util.function.UnaryOperator<:[v0]>'),

    ('java.util.stream.Stream<:[v0]>', ':[v0]'),
    ('java.io.DataInputStream', 'java.io.InputStream'),
    ('org.ehcache.config.CacheConfiguration', 'org.ehcache.config.CacheConfiguration<java.lang.String,java.lang.String>'),
    ('java.util.concurrent.ForkJoinPool', 'com.google.common.util.concurrent.ListeningExecutorService'),

    ('java.util.zip.ZipInputStream', 'java.io.InputStream'),
    ('boolean', 'java.util.concurrent.CountDownLatch'),
    ('java.util.concurrent.atomic.AtomicReference<Collection<RetentionLease>>', 'java.util.concurrent.atomic.AtomicReference<org.elasticsearch.index.seqno.RetentionLeases>'),
    ('java.util.Map<java.lang.String,RetentionLease>', 'RetentionLeases'),
    ('java.util.function.Supplier<Collection<RetentionLease>>', 'java.util.function.Supplier<org.elasticsearch.index.seqno.RetentionLeases>'),
    ('java.util.Collection<org.elasticsearch.index.seqno.RetentionLease>', 'org.elasticsearch.index.seqno.RetentionLeases'),
    ('java.util.function.BiConsumer<Collection<RetentionLease>,:[v0]>', 'java.util.function.BiConsumer<org.elasticsearch.index.seqno.RetentionLeases,:[v0]>'),
    ('org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('org.elasticsearch.search.aggregations.bucket.terms.Terms.Order', 'org.elasticsearch.search.aggregations.BucketOrder'),
    ('org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('org.elasticsearch.index.get.GetField', 'org.elasticsearch.common.document.DocumentField'),
    ('java.io.ByteArrayOutputStream', 'java.io.StringWriter'),
    ('boolean', 'org.elasticsearch.action.support.WriteRequest.RefreshPolicy'),

    ('java.util.function.Supplier<java.lang.Long>', 'long'),

    ('Result', 'boolean'),
    ('int', 'java.lang.ProcessBuilder.Redirect.Type'),
    ('java.lang.String', 'org.springframework.http.ResponseEntity'),
    ('java.lang.String', 'Component'),
    ('net.sf.ehcache.Cache', 'net.sf.ehcache.Ehcache'),
    ('boolean', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('java.util.Map<:[v1],:[v0]>', 'com.google.common.cache.LoadingCache<:[v1],:[v0]>'),
    ('java.lang.OutOfMemoryError', 'java.lang.Throwable'),

    ('java.lang.String', 'org.apache.hadoop.fs.Path'),

    ('org.elasticsearch.common.settings.Settings', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('float', 'byte'),

    ('com.google.common.collect.ImmutableMap<:[v1],:[v0]>', 'com.google.common.collect.ImmutableMultimap<:[v1],:[v0]>'),

    ('int', 'io.netty.handler.codec.http2.Http2Stream'),
    ('boolean', 'java.time.Duration'),
    ('java.util.Set<:[v0]>', 'java.util.Deque<:[v0]>'),
    ('java.util.Map<:[v0],java.lang.Integer>', 'java.util.Map<:[v0],:[v0]>'),
    ('java.lang.reflect.Field', 'java.lang.String'),
    ('com.carrotsearch.hppc.cursors.ObjectCursor<:[v0]>', ':[v0]'),
    ('java.util.concurrent.CopyOnWriteArrayList<:[v0]>', 'java.util.concurrent.CopyOnWriteArraySet<:[v0]>'),
    ('java.util.concurrent.Future<:[v0]>', 'java.util.concurrent.CompletionStage<:[v0]>'),
    ('boolean', 'java.util.EnumSet<Option>'),
    ('java.util.TimeZone', 'java.lang.String'),
    (':[v0]', 'com.google.common.collect.ImmutableMap<:[v0],:[v0]>'),
    ('android.widget.LinearLayout', 'android.widget.FrameLayout'),
    ('java.util.Map<java.lang.String,java.lang.String>', 'boolean'),
    ('java.util.List<java.lang.String>', 'java.util.List<java.net.URL>'),
    ('com.google.protobuf.ByteString', 'boolean'),
    ('boolean', 'java.util.Optional<java.lang.Long>'),
    ('java.util.Date', 'int'),
    ('com.github.javaparser.ast.modules.ModuleRequiresStmt', 'com.github.javaparser.ast.modules.ModuleRequiresDirective'),
    ('org.apache.lucene.index.IndexWriter', 'MergeSource'),
    ('java.lang.String', 'Translog.Snapshot'),
    ('com.netflix.discovery.EurekaClient', 'org.springframework.cloud.client.discovery.DiscoveryClient'),


    ('long', 'java.lang.Thread'),
    ('java.util.function.BiConsumer<org.elasticsearch.index.seqno.RetentionLeases,ActionListener<ReplicationResponse>>', 'org.elasticsearch.index.seqno.RetentionLeaseSyncer'),
    ('TransportWriteAction.WriteReplicaResult', 'TransportWriteAction.WriteReplicaResult<RetentionLeaseSyncAction.Request>'),
    ('int', 'View'),
    ('java.util.Map<:[v0],java.util.concurrent.atomic.AtomicLong>', 'java.util.Map<:[v0],java.util.concurrent.atomic.LongAdder>'),
    ('java.lang.String', 'java.util.List<java.lang.Integer>'),
    ('org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('org.elasticsearch.action.ActionListener<org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryResponse>', 'org.elasticsearch.action.ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse>'),
    ('org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('org.elasticsearch.action.ActionListener<org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse>', 'org.elasticsearch.action.ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse>'),
    ('org.elasticsearch.action.ActionListener<org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse>', 'org.elasticsearch.action.ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse>'),
    ('org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotResponse', 'org.elasticsearch.action.support.master.AcknowledgedResponse'),
    ('org.elasticsearch.cluster.routing.RoutingNode', 'org.elasticsearch.cluster.node.DiscoveryNode'),
    ('org.elasticsearch.cluster.node.DiscoveryNodes', 'org.elasticsearch.cluster.routing.allocation.RoutingAllocation'),
    ('java.util.List<ListenableFuture<Void>>', 'java.util.List<Future<Void>>'),


    ('AsyncShardFetch<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards>', 'AsyncShardFetch<org.elasticsearch.gateway.TransportNodesListGatewayStartedShards.NodeGatewayStartedShards>'),
    ('TransportNodesListGatewayMetaState', 'org.elasticsearch.client.node.NodeClient'),
    ('TransportShardRefreshAction', 'org.elasticsearch.client.node.NodeClient'),
    ('TransportReplicationAction', 'org.elasticsearch.action.ActionType<ShardResponse>'),
    ('java.util.concurrent.ConcurrentMap<:[v0],AsyncShardFetch<TransportNodesListGatewayStartedShards.NodeGatewayStartedShards>>', 'java.util.concurrent.ConcurrentMap<:[v0],AsyncShardFetch<NodeGatewayStartedShards>>'),
    ('java.text.DateFormat', 'org.joda.time.format.DateTimeFormatter'),
    ('java.util.function.Supplier<:[v0]>', 'java.util.function.Function<:[v0],:[v0]>'),
    ('net.minecraftforge.fluids.FluidTankInfo', 'net.minecraftforge.fluids.capability.IFluidTankProperties'),
    ('com.amazonaws.services.kinesis.AmazonKinesisClient', 'com.amazonaws.services.kinesis.AmazonKinesis'),
    ('float', 'java.lang.Double'),
    ('java.lang.reflect.Type', 'java.lang.Class'),

    ('int', 'org.eclipse.jetty.server.Server'),


    ('java.lang.String', 'Resource'),
    ('ShardStateAction.Listener', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('java.util.concurrent.ConcurrentMap<:[v0],CompositeListener>', 'org.elasticsearch.transport.TransportRequestDeduplicator<:[v0]>'),
    ('Listener', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),

    ('org.json.JSONObject', 'com.google.gson.JsonObject'),
    ('android.content.pm.ActivityInfo', 'android.content.pm.ComponentInfo'),
    ('java.lang.Number', 'java.math.BigDecimal'),
    ('java.lang.Long', 'java.math.BigDecimal'),


    ('long', 'java.util.function.Consumer<ActionListener<RetentionLease>>'),
    ('com.google.common.collect.Multimap<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.Set<java.lang.Integer>', 'int'),
    ('java.util.function.Consumer<:[v0]>', 'java.util.function.BiConsumer<:[v0],java.lang.Boolean>'),
    ('java.io.ObjectInput', 'java.io.DataInput'),
    ('java.io.ObjectOutput', 'java.io.DataOutput'),
    ('java.security.acl.Group', 'SimpleGroup'),
    ('boolean', 'Status'),
    ('java.sql.PreparedStatement', 'java.sql.Statement'),

    (':[v0]', 'android.util.Pair<java.lang.Boolean,:[v0]>'),
    ('org.apache.hadoop.fs.Path', 'org.apache.hadoop.yarn.api.records.URL'),
    ('java.util.Iterator', 'java.util.Iterator<Map.Entry<Object,Object>>'),
    ('java.util.List', 'java.util.List<java.util.List<Object>>'),
    ('java.lang.Float', 'java.lang.String'),
    ('java.util.List<:[v0]>', 'java.util.function.Consumer<:[v0]>'),
    ('java.util.Properties', 'boolean'),

    ('java.math.BigDecimal', 'java.math.BigInteger'),
    ('java.lang.String', 'User'),
    ('java.lang.reflect.Type', 'com.google.common.reflect.TypeToken<T>'),
    ('org.apache.hadoop.fs.FileStatus', 'java.lang.String'),
    ('org.eclipse.jetty.util.ConcurrentHashSet<:[v0]>', 'java.util.Set<:[v0]>'),

    ('java.lang.reflect.Method', 'java.lang.reflect.Executable'),
    ('com.sun.source.tree.ExpressionTree', 'java.lang.String'),

    ('java.util.Collection<com.google.devtools.build.lib.vfs.Path>', 'java.util.Collection<java.lang.String>'),
    ('java.util.List', 'java.util.Set<java.lang.String>'),
    ('com.github.javaparser.ast.stmt.ForeachStmt', 'com.github.javaparser.ast.stmt.ForEachStmt'),
    ('com.github.javaparser.ast.stmt.SwitchEntryStmt', 'com.github.javaparser.ast.stmt.SwitchEntry'),


    ('java.util.Optional<T>', 'java.util.Optional<V>'),

    ('java.util.List<java.lang.Double>', 'java.util.List<java.lang.Number>'),
    (':[v0]', 'java.lang.Comparable<:[v0]>'),
    ('RequestHandlerRegistry', 'RequestHandlerRegistry<T>'),
    ('com.mongodb.BasicDBObject', 'org.bson.Document'),

    ('org.skife.jdbi.v2.DBI', 'org.jdbi.v3.core.Jdbi'),
    ('java.lang.Void', 'boolean'),
    ('com.google.api.client.googleapis.auth.oauth2.GoogleCredential', 'com.google.auth.oauth2.GoogleCredentials'),
    ('com.sun.source.tree.VariableTree', 'com.sun.source.util.TreePath'),
    ('java.util.List<:[v0]>', 'ImmutableList<:[v0]>'),
    ('org.apache.http.HttpResponse', 'int'),
    ('org.asynchttpclient.HttpResponseHeaders', 'io.netty.handler.codec.http.HttpHeaders'),
    ('org.reactivestreams.Publisher<:[v0]>', 'java.util.List<:[v0]>'),
    ('java.io.BufferedInputStream', 'java.io.InputStream'),
    ('com.google.common.collect.LinkedHashMultimap<:[v1],:[v0]>', 'com.google.common.collect.ListMultimap<:[v1],:[v0]>'),
    ('java.lang.invoke.LambdaFormEditor.Transform.Kind', 'boolean'),
    ('java.lang.Class<B>', 'java.lang.Class<T2>'),
    ('java.lang.Class<A>', 'java.lang.Class<T1>'),

    ('Statement', 'Node'),
    ('org.eclipse.jetty.server.session.HashSessionManager', 'org.eclipse.jetty.server.session.SessionHandler'),
    ('org.apache.lucene.search.BooleanQuery', 'org.apache.lucene.search.Query'),
    ('java.lang.String', 'Violation'),
    ('java.util.Set<Entry<Object,Object>>', 'java.util.Set<Map.Entry<Object,Object>>'),
    ('Operation', 'java.lang.String'),
    ('com.fasterxml.jackson.databind.JsonNode', 'com.fasterxml.jackson.databind.node.ObjectNode'),
    ('Datum', 'IFacade'),
    ('java.util.Map<java.lang.Integer,:[v0]>', 'java.util.concurrent.atomic.AtomicReferenceArray<:[v0]>'),
    ('org.apache.logging.log4j.Logger', 'java.util.function.Predicate<java.lang.Throwable>'),
    ('java.util.Optional<java.lang.Error>', 'java.util.Optional<T>'),
    ('java.util.Optional<java.lang.String>', 'boolean'),
    ('java.io.File', 'java.io.OutputStream'),
    ('java.lang.Class', 'java.lang.Class<E>'),
    ('java.util.concurrent.atomic.LongAdder', 'java.util.concurrent.atomic.AtomicInteger'),
    ('java.util.HashSet<:[v0]>', 'java.util.SortedSet<:[v0]>'),
    ('buildcraft.lib.expression.Expression', 'buildcraft.lib.expression.api.IExpression'),

    ('java.math.BigInteger', 'int'),
    ('boolean', 'java.util.concurrent.CompletionStage<java.lang.Boolean>'),

    (':[v0]', 'Map.Entry<:[v0],List<String>>'),
    ('java.net.URI', 'int'),
    ('org.opensaml.saml.metadata.resolver.impl.BasicRoleDescriptorResolver', 'org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver'),
    ('java.lang.String', 'org.springframework.http.HttpHeaders'),
    ('io.netty.buffer.ByteBuf', 'java.nio.ByteBuffer'),
    ('org.json.JSONArray', 'com.fasterxml.jackson.databind.node.ArrayNode'),
    ('com.mongodb.DBObject', 'org.bson.conversions.Bson'),
    ('java.util.List<Map<String,Object>>', 'int'),
    ('java.lang.Class', 'java.lang.Class<java.lang.Number>'),
    ('java.lang.reflect.Constructor', 'java.lang.reflect.Constructor<C>'),


    ('java.util.List<:[v0]>', 'java.util.Map<:[v0],java.lang.Boolean>'),
    ('java.util.HashMap<java.lang.String,:[v0]>', 'java.util.List<:[v0]>'),



    ('java.security.MessageDigest', 'javax.crypto.Mac'),
    ('java.util.concurrent.CompletionStage<:[v0]>', ':[v0]'),

    ('java.lang.Long', 'java.lang.Boolean'),
    ('java.util.List<:[v0]>', 'java.util.Map<:[v0],java.lang.Integer>'),
    ('com.google.common.collect.Multiset<:[v0]>', 'java.util.Map<:[v0],java.lang.Integer>'),
    ('org.elasticsearch.index.fielddata.SortedNumericDoubleValues', 'org.apache.lucene.index.SortedNumericDocValues'),
    ('java.time.LocalDate', 'java.time.LocalTime'),


    ('java.util.Map', 'java.util.concurrent.ConcurrentMap'),
    ('SendMetricListener', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('java.lang.Runnable', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('SendListener', 'ReleaseListener'),
    ('java.io.Closeable', 'long'),

    ('org.elasticsearch.cluster.SnapshotsInProgress.State', 'org.elasticsearch.cluster.SnapshotsInProgress.ShardState'),
    ('javax.servlet.http.HttpServletRequest', 'javax.servlet.ServletRequest'),

    ('java.util.Properties', 'int'),
    ('android.content.pm.ComponentInfo', 'android.content.pm.ActivityInfo'),
    ('MoveEntityEvent.Position.Teleport.Portal', 'MoveEntityEvent.Teleport.Portal'),

    ('Expression', 'int'),

    ('TransportResponseOptions', 'boolean'),
    ('org.elasticsearch.common.io.stream.Writeable', 'Response'),
    ('InboundMessage.ResponseMessage', 'InboundMessage.Response'),
    ('TcpTransport', 'org.elasticsearch.indices.breaker.CircuitBreakerService'),
    ('Message', 'Response'),
    ('InboundMessage.RequestMessage', 'InboundMessage.Request'),
    ('java.util.List<org.elasticsearch.common.transport.InetSocketTransportAddress>', 'java.util.List<org.elasticsearch.common.transport.TransportAddress>'),
    ('org.apache.lucene.index.SortedDocValues', 'org.apache.lucene.index.SortedSetDocValues'),
    ('java.util.List<:[v0]>', 'java.util.concurrent.LinkedBlockingQueue<:[v0]>'),


    ('java.net.URL', 'java.nio.file.Path'),
    ('java.io.File', 'java.util.List<java.lang.String>'),
    ('java.util.Map.Entry<:[v0],java.lang.Integer>', ':[v0]'),
    ('javax.servlet.http.Mapping', 'javax.servlet.http.ServletMapping'),
    ('org.apache.shiro.mgt.SecurityManager', 'org.apache.shiro.mgt.DefaultSecurityManager'),
    ('org.elasticsearch.common.bytes.ReleasablePagedBytesReference', 'org.elasticsearch.common.bytes.ReleasableBytesReference'),
    ('java.lang.String', 'java.text.MessageFormat'),
    ('java.util.List<TcpChannel>', 'java.util.List<TcpServerChannel>'),
    ('TcpChannel', 'TcpServerChannel'),
    ('NettyTcpChannel', 'Netty4TcpServerChannel'),
    ('io.netty.util.AttributeKey<NettyTcpChannel>', 'io.netty.util.AttributeKey<Netty4TcpChannel>'),
    ('Map.Entry<:[v0],List<TcpChannel>>', 'Map.Entry<:[v0],List<TcpServerChannel>>'),
    ('java.util.Map<:[v0],List<TcpChannel>>', 'java.util.Map<:[v0],List<TcpServerChannel>>'),
    ('NettyTcpChannel', 'Netty4TcpChannel'),
    ('io.netty.util.Attribute<NettyTcpChannel>', 'io.netty.util.Attribute<Netty4TcpChannel>'),
    ('java.util.Map<:[v0],java.lang.Long>', 'java.util.Map<:[v0],java.lang.Integer>'),

    ('java.util.Map<:[v1],:[v0]>', 'java.util.NavigableMap<:[v1],:[v0]>'),
    ('com.google.protobuf.ByteString', 'java.nio.ByteBuffer'),
    ('Map.Entry<java.lang.Integer,:[v0]>', 'Map.Entry<java.lang.Long,:[v0]>'),
    ('java.io.File', 'org.apache.hadoop.fs.FileStatus'),
    ('org.drools.compiler.compiler.io.Resource', 'org.drools.compiler.compiler.io.FileSystemItem'),
    ('java.lang.String', 'java.util.List<java.lang.Double>'),
    ('org.w3c.dom.Node', 'java.lang.String'),
    ('java.util.concurrent.ConcurrentHashMap<:[v0],java.lang.Boolean>', 'java.util.Set<:[v0]>'),
    ('java.util.Deque<:[v0]>', 'java.util.Stack<:[v0]>'),
    ('java.lang.Runnable', 'java.util.function.Consumer<T>'),
    ('com.google.common.cache.LoadingCache<:[v1],:[v0]>', 'java.util.Map<:[v1],:[v0]>'),
    ('java.util.Map<Set<Role>,:[v0]>', 'java.util.Map<Set<DiscoveryNodeRole>,:[v0]>'),
    ('java.util.Set<DiscoveryNode.Role>', 'java.util.Set<org.elasticsearch.cluster.node.DiscoveryNodeRole>'),
    ('DiscoveryNode.Role', 'org.elasticsearch.cluster.node.DiscoveryNodeRole'),
    ('java.util.List<DiscoveryNode.Role>', 'java.util.List<org.elasticsearch.cluster.node.DiscoveryNodeRole>'),
    ('org.elasticsearch.cluster.node.DiscoveryNode.Role', 'DiscoveryNodeRole'),
    ('java.util.Set<Role>', 'java.util.Set<DiscoveryNodeRole>'),
    ('java.util.Map<DiscoveryNode.Role,:[v0]>', 'java.util.Map<org.elasticsearch.cluster.node.DiscoveryNodeRole,:[v0]>'),

    ('ImmutableMap.Builder<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    ('java.util.concurrent.ConcurrentSkipListMap<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),

    ('org.spongepowered.api.world.extent.ImmutableBiomeArea', 'org.spongepowered.api.world.extent.ImmutableBiomeVolume'),

    ('WriteReplicaResult', 'WriteReplicaResult<ResyncReplicationRequest>'),
    ('java.lang.Short', 'java.lang.Integer'),
    ('ImmutableSet.Builder<:[v0]>', 'java.util.function.Consumer<:[v0]>'),
    ('java.util.Map<:[v1],:[v0]>', 'android.util.LruCache<:[v1],:[v0]>'),
    ('io.netty.buffer.CompositeByteBuf', 'io.netty.buffer.ByteBuf'),
    ('java.util.Stack<:[v0]>', 'java.util.List<:[v0]>'),
    ('Transport.Connection', 'org.elasticsearch.common.util.concurrent.ListenableFuture<java.lang.Void>'),
    ('io.reactivex.Single<:[v0]>', 'io.reactivex.Maybe<:[v0]>'),
    ('java.net.URL', 'java.io.InputStream'),
    ('java.lang.String', 'java.io.OutputStream'),
    ('java.util.List<IndexId>', 'ShardGenerations'),

    ('java.time.LocalDateTime', 'java.time.Instant'),
    ('int', 'java.util.function.Supplier<java.lang.Integer>'),
    ('java.net.URI', 'java.nio.file.Path'),
    ('java.util.List<:[v0]>', 'com.google.common.collect.ImmutableList.Builder<:[v0]>'),
    ('java.util.concurrent.ConcurrentMap<java.lang.Long,:[v0]>', 'java.util.concurrent.ConcurrentMap<java.lang.String,:[v0]>'),
    ('io.netty.channel.Channel', 'io.netty.channel.embedded.EmbeddedChannel'),
    ('java.util.function.Consumer<java.lang.Void>', 'java.lang.Runnable'),
    ('Listener', 'org.elasticsearch.action.ActionListener<AllocateDangledResponse>'),
    ('org.elasticsearch.cluster.routing.allocation.AllocationService', 'java.util.function.BiFunction<org.elasticsearch.cluster.ClusterState,java.lang.String,org.elasticsearch.cluster.ClusterState>'),
    ('java.util.function.Consumer<:[v0]>', 'java.util.function.BiConsumer<:[v0],ActionListener<Void>>'),

    ('java.io.InputStream', 'java.io.File'),
    ('java.util.Map<:[v0],java.lang.Integer>', 'java.util.List<:[v0]>'),
    ('java.lang.String', 'GitInfo'),

    ('java.net.CookieManager', 'java.net.CookieHandler'),
    ('boolean', 'Mode'),
    ('java.util.List<:[v0]>', 'org.apache.flink.api.common.state.ListState<:[v0]>'),
    ('ImmutableMultimap.Builder<:[v1],:[v0]>', 'ImmutableListMultimap.Builder<:[v1],:[v0]>'),
    ('java.lang.String', 'Tag'),
    ('boolean', 'java.net.InetSocketAddress'),
    ('org.apache.lucene.index.IndexReader', 'org.elasticsearch.common.lucene.index.ElasticsearchDirectoryReader'),
    ('java.util.function.BiFunction<:[v1],:[v0],Searcher>', 'java.util.function.BiFunction<:[v1],:[v0],Engine.Searcher>'),
    ('ExternalSearcherManager', 'ExternalReaderManager'),
    ('org.apache.lucene.search.IndexSearcher', 'org.elasticsearch.common.lucene.index.ElasticsearchDirectoryReader'),
    ('org.apache.lucene.index.DirectoryReader', 'org.elasticsearch.common.lucene.index.ElasticsearchDirectoryReader'),
    ('org.apache.lucene.search.ReferenceManager<org.apache.lucene.search.IndexSearcher>', 'org.apache.lucene.search.ReferenceManager<org.elasticsearch.common.lucene.index.ElasticsearchDirectoryReader>'),
    ('org.apache.lucene.search.SearcherManager', 'ElasticsearchReaderManager'),
    ('com.google.common.collect.ImmutableSortedMap<:[v0],:[v1]>', 'java.util.Map<:[v0],:[v1]>'),
    ('org.elasticsearch.action.Action<:[v0]>', 'org.elasticsearch.action.ActionType<:[v0]>'),
    ('java.util.Map<org.elasticsearch.action.Action,:[v0]>', 'java.util.Map<org.elasticsearch.action.ActionType,:[v0]>'),
    ('org.elasticsearch.common.inject.multibindings.MapBinder<Action,:[v0]>', 'org.elasticsearch.common.inject.multibindings.MapBinder<ActionType,:[v0]>'),
    ('java.util.concurrent.CyclicBarrier', 'java.util.concurrent.CountDownLatch'),
    ('java.util.List<:[v0]>', 'com.google.common.collect.Multimap<java.lang.String,:[v0]>'),
    ('ImmutableSet.Builder<:[v0]>', 'ImmutableSortedSet.Builder<:[v0]>'),
    ('org.w3c.dom.Node', 'org.w3c.dom.Element'),
    ('java.util.Map<:[v1],:[v1]>', 'com.google.common.collect.ImmutableMap<:[v0],:[v0]>'),
    ('org.elasticsearch.action.ActionListener<org.elasticsearch.transport.TransportResponse.Empty>', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('org.elasticsearch.action.ActionListener<Response>', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('java.io.OutputStream', 'java.io.Writer'),
    ('java.util.Set<:[v0]>', 'java.util.Map<:[v0],org.elasticsearch.cluster.block.ClusterBlock>'),
    ('com.mongodb.WriteResult', 'com.mongodb.client.result.UpdateResult'),
    ('java.lang.String', 'Command'),
    ('java.io.BufferedReader', 'java.io.Reader'),
    ('java.io.StringReader', 'java.io.Reader'),
    ('java.util.concurrent.BlockingQueue<:[v0]>', 'java.util.concurrent.BlockingDeque<:[v0]>'),
    ('javax.lang.model.element.Element', 'javax.lang.model.AnnotatedConstruct'),
    ('javax.lang.model.element.Element', 'com.sun.tools.javac.code.Symbol'),
    (':[v0]', 'Map.Entry<java.lang.Long,:[v0]>'),
    ('java.lang.Boolean', 'java.util.Set<java.lang.String>'),
    ('org.reactivestreams.Subscriber<:[v0]>', 'reactor.core.CoreSubscriber<:[v0]>'),
    ('java.security.Key', 'java.security.PrivateKey'),
    ('com.google.common.collect.ImmutableMap<java.lang.String,:[v0]>', ':[v0]'),
    ('ImmutableMap.Builder<:[v1],:[v0]>', 'ImmutableSortedMap.Builder<:[v1],:[v0]>'),
    (':[v0]', 'java.util.Map.Entry<:[v0],java.lang.Integer>'),
    ('com.fasterxml.jackson.databind.JsonNode', 'java.lang.String'),
    ('java.lang.Error', 'java.lang.Throwable'),
    ('android.content.pm.ApplicationInfo', 'int'),
    ('java.lang.Integer', 'double'),
    ('java.lang.Long', 'double'),
    ('org.pac4j.core.context.session.SessionStore<org.pac4j.core.context.J2EContext>', 'org.pac4j.core.context.session.SessionStore<org.pac4j.core.context.JEEContext>'),
    ('org.pac4j.core.context.J2EContext', 'org.pac4j.core.context.JEEContext'),
    ('zipkin2.Callback<java.lang.Void>', 'zipkin2.Callback<java.lang.Integer>'),
    ('org.junit.jupiter.api.extension.TestExtensionContext', 'org.junit.jupiter.api.extension.ExtensionContext'),
    ('org.junit.jupiter.api.extension.ContainerExtensionContext', 'org.junit.jupiter.api.extension.ExtensionContext'),
    ('StaticWithToBuilder', 'StaticWithToBuilder<Z,java.lang.String>'),
    ('IndexType', 'java.lang.String'),
    ('java.util.List<java.nio.file.Path>', 'java.util.List<java.lang.String>'),
    ('org.pac4j.core.http.CallbackUrlResolver', 'org.pac4j.core.http.UrlResolver'),
    ('org.apache.commons.httpclient.methods.PutMethod', 'org.apache.http.client.methods.HttpPut'),
    ('com.google.common.util.concurrent.SettableFuture<java.lang.Void>', 'com.google.common.util.concurrent.SettableFuture<java.lang.Boolean>'),




    ('java.util.Enumeration<:[v0]>', 'java.util.Set<:[v0]>'),



    ('org.I0Itec.zkclient.ZkClient', 'kafka.utils.ZkUtils'),
    ('NodeChannels', 'org.elasticsearch.common.lease.Releasable'),
    ('org.elasticsearch.action.ActionListener<NodeChannels>', 'org.elasticsearch.action.ActionListener<Transport.Connection>'),
    ('org.elasticsearch.action.support.PlainActionFuture<NodeChannels>', 'org.elasticsearch.action.ActionListener<Transport.Connection>'),
    ('Connection', 'org.elasticsearch.common.lease.Releasable'),
    ('com.google.common.collect.ImmutableSet<java.lang.String>', 'com.google.common.collect.ImmutableSet<java.nio.file.Path>'),
    ('java.util.concurrent.ScheduledExecutorService', 'com.google.common.util.concurrent.ListeningExecutorService'),

    ('java.io.PrintWriter', 'java.io.BufferedWriter'),
    ('java.util.List<java.lang.String>', 'java.nio.file.Path'),
    ('java.util.Iterator<:[v0]>', 'CloseableIterator<:[v0]>'),
    ('java.util.Iterator<java.lang.String>', 'java.util.Iterator<T>'),
    ('org.elasticsearch.action.support.PlainActionFuture<java.lang.Void>', 'org.elasticsearch.action.support.PlainActionFuture<NodeChannels>'),

    ('javax.inject.Provider<:[v0]>', ':[v0]'),

    ('org.elasticsearch.common.collect.ImmutableOpenMap<:[v0],org.elasticsearch.common.collect.ImmutableOpenMap<String,MappingMetaData>>', 'org.elasticsearch.common.collect.ImmutableOpenMap<:[v0],org.elasticsearch.common.collect.ImmutableOpenMap<String,MappingMetadata>>'),
    ('org.elasticsearch.cluster.metadata.MappingMetaData', 'org.elasticsearch.cluster.metadata.MappingMetadata'),
    ('javax.lang.model.element.ExecutableElement', 'java.lang.String'),
    ('android.widget.FrameLayout', 'android.widget.LinearLayout'),
    ('java.lang.Class<T>', 'java.lang.reflect.Type'),

    ('com.google.common.cache.LoadingCache<:[v1],:[v0]>', 'com.google.common.cache.Cache<:[v1],:[v0]>'),

    ('java.util.Map.Entry<java.lang.Long,:[v0]>', ':[v0]'),
    ('java.lang.reflect.Method', 'java.lang.reflect.AnnotatedElement'),
    ('com.google.common.collect.ImmutableMultimap<:[v1],:[v0]>', 'com.google.common.collect.ImmutableSetMultimap<:[v1],:[v0]>'),
    ('com.sun.tools.javac.api.JavacTaskImpl', 'com.sun.source.util.JavacTask'),
    ('android.widget.FrameLayout', 'android.view.View'),
    ('java.text.DateFormat', 'java.time.format.DateTimeFormatter'),

    ('boolean', 'javax.servlet.http.HttpServletRequest'),


    ('java.io.InputStream', 'java.io.DataInputStream'),
    ('org.elasticsearch.action.ActionListener<java.lang.Void>', 'org.elasticsearch.action.ActionListener<java.lang.String>'),
    ('org.elasticsearch.action.StepListener<java.lang.Void>', 'org.elasticsearch.action.StepListener<java.lang.String>'),
    ('java.util.Map<:[v0],org.elasticsearch.common.blobstore.BlobMetaData>', 'java.util.Set<:[v0]>'),
    ('java.io.InputStream', 'com.amazonaws.services.s3.model.S3ObjectInputStream'),
    ('java.lang.String', 'com.fasterxml.jackson.databind.node.ObjectNode'),
    ('com.google.common.base.Predicate<:[v0]>', 'java.util.function.Predicate<:[v0]>'),
    ('java.util.function.Function<java.lang.Integer,java.lang.Integer>', 'java.util.function.IntUnaryOperator'),
    ('java.util.Optional<java.io.File>', 'java.util.Optional<java.nio.file.Path>'),
    ('org.pac4j.core.authorization.generator.AuthorizationGenerator<org.pac4j.core.profile.CommonProfile>', 'org.pac4j.core.authorization.generator.AuthorizationGenerator'),
    ('com.codahale.metrics.Meter', 'com.codahale.metrics.Counter'),
    ('java.util.List<T>', 'java.util.List<E>'),
    ('net.openhft.chronicle.queue.ChronicleQueueBuilder', 'net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder'),
    ('java.lang.Class<T>', 'java.lang.Class<C>'),
    ('java.util.Collection<DeleteVersionValue>', 'java.util.Map<org.apache.lucene.util.BytesRef,VersionValue>'),
    ('com.google.common.collect.ImmutableSet', 'java.util.Set'),
    ('org.springframework.http.ResponseEntity', 'org.springframework.http.ResponseEntity<java.lang.Void>'),
    ('java.io.FileInputStream', 'java.io.File'),
    ('java.util.concurrent.ConcurrentLinkedQueue<:[v0]>', 'java.util.Queue<:[v0]>'),
    ('java.io.File', 'org.junit.rules.TemporaryFolder'),
    ('SnapshotShards', 'java.util.Map<org.elasticsearch.index.shard.ShardId,org.elasticsearch.index.snapshots.IndexShardSnapshotStatus>'),
    ('java.lang.String', 'java.lang.reflect.Member'),
    ('int', 'Stage'),
    ('java.util.function.LongSupplier', 'java.time.Clock'),
    ('org.pac4j.saml.client.SAML2ClientConfiguration', 'org.pac4j.saml.config.SAML2Configuration'),

    ('java.util.Map<:[v1],:[v1]>', ':[v0]'),
    ('org.elasticsearch.http.HttpServer', 'org.elasticsearch.http.HttpServerTransport'),

    ('org.pac4j.core.credentials.authenticator.Authenticator<org.pac4j.core.credentials.TokenCredentials>', 'org.pac4j.core.credentials.authenticator.Authenticator'),
    ('android.content.Context', 'android.view.View'),
    ('java.util.List<:[v0]>', 'java.util.ListIterator<:[v0]>'),
    ('java.util.Random', 'java.util.concurrent.ThreadLocalRandom'),

    ('org.opensaml.messaging.context.MessageContext<org.opensaml.saml.common.SAMLObject>', 'org.opensaml.messaging.context.MessageContext'),
    ('android.widget.ImageView', 'android.widget.LinearLayout'),

    ('java.util.HashMap<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v1],:[v0]>'),
    ('javax.swing.JTextField', 'javax.swing.JComboBox<java.lang.String>'),
    ('org.ldaptive.SearchFilter', 'org.ldaptive.FilterTemplate'),
    ('org.ldaptive.auth.PooledBindAuthenticationHandler', 'org.ldaptive.auth.SimpleBindAuthenticationHandler'),
    ('java.util.concurrent.CompletableFuture', 'java.util.concurrent.CompletableFuture<java.lang.Void>'),
    ('java.util.concurrent.CompletableFuture<T>', 'java.util.concurrent.CompletableFuture<java.lang.Void>'),
    ('com.squareup.javapoet.ClassName', 'com.squareup.javapoet.TypeName'),
    ('java.lang.String', 'com.squareup.javapoet.TypeName'),
    (':[v0]', 'java.util.HashMap<java.lang.Integer,:[v0]>'),
    ('com.google.common.collect.ArrayListMultimap<:[v1],:[v0]>', 'com.google.common.collect.ListMultimap<:[v1],:[v0]>'),
    ('int', 'java.util.function.IntSupplier'),
    ('org.apache.lucene.util.Bits', 'org.apache.lucene.index.LeafReaderContext'),
    ('org.apache.lucene.index.NumericDocValues', 'long'),
    ('org.springframework.cloud.client.ServiceInstance', 'org.springframework.cloud.client.serviceregistry.Registration'),
    ('java.util.List', 'java.lang.Iterable'),
    ('java.util.concurrent.atomic.AtomicReference<:[v0]>', 'java.util.concurrent.CompletableFuture<:[v0]>'),

    ('boolean', 'java.lang.invoke.LambdaFormEditor.Transform.Kind'),
    ('org.elasticsearch.common.geo.GeoPoint', 'long'),
    ('org.elasticsearch.index.fielddata.MultiGeoPointValues', 'org.apache.lucene.index.SortedNumericDocValues'),

    ('com.sun.source.util.TreePath', 'com.sun.source.tree.ClassTree'),


    ('java.util.List<java.lang.String>', 'long'),
    ('com.google.devtools.remoteexecution.v1test.Command', 'build.bazel.remote.execution.v2.Command'),
    ('java.lang.reflect.Constructor', 'java.lang.reflect.AccessibleObject'),
    ('org.elasticsearch.action.support.PlainListenableActionFuture<java.lang.Void>', 'java.util.List<ActionListener<Void>>'),
    ('long', 'java.lang.Throwable'),
    ('org.elasticsearch.action.support.GroupedActionListener<:[v0]>', 'org.elasticsearch.action.ActionListener<:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v0],java.lang.Boolean>'),
    ('javax.jms.TextMessage', 'javax.jms.Message'),
    ('java.util.Hashtable', 'java.util.Hashtable<java.lang.String,java.lang.String>'),
    ('java.util.ArrayList<:[v0]>', 'java.util.ArrayDeque<:[v0]>'),

    ('java.io.BufferedWriter', 'java.io.BufferedOutputStream'),
    ('bool', 'boolean'),
    ('com.google.pubsub.v1.ProjectTopicName', 'com.google.pubsub.v1.TopicName'),
    ('java.lang.reflect.AnnotatedElement', 'java.lang.reflect.Member'),
    ('java.util.function.Function<java.lang.Double,java.lang.Double>', 'java.util.function.DoubleUnaryOperator'),
    ('java.util.TimerTask', 'java.lang.Runnable'),
    ('org.joda.time.DateTimeZone', 'java.time.ZoneOffset'),

    ('java.nio.file.Path', 'Result'),
    ('android.support.v7.widget.GridLayoutManager', 'android.support.v7.widget.LinearLayoutManager'),
    ('java.util.function.Function<java.lang.String,:[v0]>', ':[v0]'),
    (':[v0]', 'java.util.PriorityQueue<:[v0]>'),

    ('org.elasticsearch.index.store.IndexStore', 'IndexStorePlugin.DirectoryFactory'),
    ('org.elasticsearch.index.store.FsDirectoryService', 'org.apache.lucene.store.Directory'),
    ('java.util.Map<:[v0],Function<IndexSettings,IndexStore>>', 'java.util.Map<:[v0],IndexStorePlugin.DirectoryFactory>'),
    ('java.math.BigDecimal', 'long'),
    ('java.util.Iterator', 'java.util.Iterator<java.io.File>'),


    ('com.squareup.okhttp.OkHttpClient', 'okhttp3.OkHttpClient'),
    ('org.elasticsearch.client.Client', 'java.lang.String'),
    ('org.apache.commons.httpclient.HttpMethod', 'org.apache.http.client.methods.HttpGet'),
    ('com.vividsolutions.jts.geom.Polygon', 'org.locationtech.jts.geom.Polygon'),
    (':[v0]', 'rx.Observable<:[v0]>'),
    ('org.springframework.beans.factory.BeanFactory', 'org.springframework.beans.factory.config.ConfigurableBeanFactory'),
    ('java.util.LinkedList', 'java.util.List'),
    ('java.lang.String', 'org.springframework.core.io.Resource'),
    ('java.util.function.Consumer<ActionListener<RetentionLease>>', 'long'),

    ('javafx.collections.ObservableList<:[v0]>', ':[v0]'),
    ('org.elasticsearch.index.shard.ShardId', 'NodeRequest'),
    ('org.elasticsearch.index.IndexSettings', 'java.lang.String'),
    ('org.springframework.web.client.RestTemplate', 'org.springframework.web.client.RestOperations'),
    ('java.util.Set<Map.Entry<K,V>>', 'java.util.Set<Entry<K,V>>'),
    ('com.squareup.okhttp.OkHttpClient', 'OkHttpClient.Builder'),
    ('java.net.URL', 'okhttp3.HttpUrl'),
    ('java.io.Writer', 'java.io.BufferedWriter'),

    (':[v0]', 'java.util.function.UnaryOperator<:[v0]>'),
    ('redis.clients.jedis.Jedis', 'io.lettuce.core.api.sync.RedisCommands<java.lang.String,java.lang.String>'),
    ('net.sf.json.JSON', 'java.lang.String'),
    ('org.apache.log4j.spi.LoggingEvent', 'org.apache.logging.log4j.core.LogEvent'),
    ('java.lang.Iterable<:[v0]>', 'com.google.common.collect.FluentIterable<:[v0]>'),



    ('java.lang.String', 'Dependency'),
    ('java.util.function.Function<java.lang.Integer,:[v0]>', 'java.util.function.IntFunction<:[v0]>'),
    ('RoutingService', 'BatchedRerouteService'),
    ('java.util.function.BiConsumer<java.lang.String,ActionListener<Void>>', 'org.elasticsearch.cluster.routing.RerouteService'),
    ('org.elasticsearch.cluster.routing.RoutingService', 'org.elasticsearch.cluster.routing.RerouteService'),
    ('java.util.Set<:[v0]>', 'java.util.Map<:[v0],java.lang.Long>'),
    ('char', 'long'),










    ('long', 'char'),










    ('double', 'char'),






    ('java.util.StringJoiner', 'java.lang.StringBuilder'),
    ('java.util.OptionalInt', 'java.util.OptionalLong'),
    ('org.springframework.web.util.DefaultUriTemplateHandler', 'org.springframework.web.util.DefaultUriBuilderFactory'),
    ('java.util.List<:[v0]>', 'java.util.Map<com.google.common.hash.HashCode,:[v0]>'),

    ('java.lang.Double', 'long'),
    ('java.io.OutputStream', 'java.io.DataOutputStream'),
    ('java.util.Iterator<:[v0]>', 'java.util.Set<:[v0]>'),
    ('int', 'ReturnCode'),

    ('java.lang.Integer', 'java.util.concurrent.atomic.AtomicInteger'),

    ('java.util.Collection<java.lang.String>', 'java.util.Collection<T>'),
    ('java.util.concurrent.CompletableFuture<java.lang.Boolean>', 'java.util.concurrent.CompletableFuture<T>'),
    ('java.lang.String', 'JavadocTag'),
    ('com.google.common.util.concurrent.SettableFuture<java.lang.Boolean>', 'com.google.common.util.concurrent.SettableFuture<java.lang.Void>'),
    ('java.util.function.ToIntBiFunction<:[v0],:[v1]>', 'java.util.function.ToLongBiFunction<:[v0],:[v1]>'),
    ('java.lang.String', 'Engine.HistorySource'),
    ('java.util.concurrent.locks.Lock', 'java.util.concurrent.locks.ReentrantReadWriteLock'),
    ('org.objectweb.asm.ClassWriter', 'org.objectweb.asm.ClassVisitor'),

    ('java.util.Collection<:[v0]>', 'java.util.TreeSet<:[v0]>'),
    ('java.io.RandomAccessFile', 'java.nio.channels.FileChannel'),
    ('java.util.concurrent.ArrayBlockingQueue<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>'),
    ('java.util.Set<Map.Entry>', 'java.util.Set<Map.Entry<String,String>>'),
    ('java.util.List<java.lang.Integer>', 'java.util.BitSet'),


    ('java.util.HashSet<:[v0]>', 'java.util.TreeSet<:[v0]>'),
    ('java.lang.Thread', 'long'),
    ('java.util.List<io.undertow.servlet.api.ThreadSetupAction>', 'java.util.List<io.undertow.servlet.api.ThreadSetupHandler>'),
    ('java.io.StringWriter', 'java.io.ByteArrayOutputStream'),

    ('java.util.Collection<java.lang.Long>', 'java.util.Collection<java.lang.Integer>'),
    ('java.util.Map<org.elasticsearch.action.GenericAction,:[v0]>', 'java.util.Map<org.elasticsearch.action.Action,:[v0]>'),
    ('org.elasticsearch.action.GenericAction<Request,:[v0]>', 'org.elasticsearch.action.Action<:[v0]>'),
    ('org.elasticsearch.common.inject.multibindings.MapBinder<GenericAction,:[v0]>', 'org.elasticsearch.common.inject.multibindings.MapBinder<Action,:[v0]>'),
    ('int', 'java.io.File'),

    (':[v0]', 'java.util.Map<:[e7f8341b-3df6-3344-7fb5-0c06fd2c18ee_v0_equal],:[e7f8341b-3df6-3344-7fb5-0c06fd2c18ee_v0_equal]>'),

    (':[v0]', 'Map.Entry<:[v0],java.lang.Integer>'),
    ('com.google.gson.JsonObject', 'com.fasterxml.jackson.databind.JsonNode'),
    ('com.google.gson.JsonElement', 'com.fasterxml.jackson.databind.JsonNode'),
    ('com.google.gson.JsonArray', 'com.fasterxml.jackson.databind.JsonNode'),
    ('com.google.gson.Gson', 'com.fasterxml.jackson.databind.ObjectMapper'),
    ('java.util.LinkedList<:[v0]>', 'java.util.concurrent.ConcurrentLinkedQueue<:[v0]>'),
    ('org.apache.flink.api.java.ExecutionEnvironment', 'org.apache.flink.streaming.api.environment.StreamExecutionEnvironment'),
    ('com.amazonaws.auth.AWSCredentials', 'com.amazonaws.auth.AWSCredentialsProvider'),
    ('org.elasticsearch.threadpool.ThreadPool', 'java.util.concurrent.Executor'),
    ('boolean', 'MatchResult'),
    ('org.apache.http.impl.client.DefaultHttpClient', 'org.apache.http.client.HttpClient'),
    ('java.util.concurrent.CompletableFuture<java.lang.Void>', 'java.util.concurrent.CompletableFuture<Result>'),
    ('org.elasticsearch.cluster.DiskUsage', 'DiskUsageWithRelocations'),

    ('java.util.regex.Pattern', 'com.google.common.base.Splitter'),
    ('org.apache.hadoop.hbase.client.HConnection', 'org.apache.hadoop.hbase.client.Connection'),
    ('java.util.concurrent.ConcurrentMap<:[v1],:[v0]>', 'com.github.benmanes.caffeine.cache.Cache<:[v1],:[v0]>'),
    ('java.util.Set<:[v0]>', 'java.util.Optional<:[v0]>'),
    ('java.lang.Integer', 'java.time.Duration'),
    ('java.io.File', 'java.io.Writer'),

    ('java.util.Vector', 'java.util.List'),
    ('java.lang.reflect.Method', 'int'),
    ('java.util.Iterator<java.lang.Integer>', 'java.util.Iterator<java.lang.String>'),
    ('java.util.Queue<:[v0]>', 'java.util.concurrent.ArrayBlockingQueue<:[v0]>'),
    ('java.util.function.Function<:[v0],R>', 'java.util.function.Function<:[v0],:[v0]>'),

    ('java.util.concurrent.ConcurrentHashMap<:[v0],:[v1]>', 'java.util.concurrent.ConcurrentMap<:[v0],:[v1]>'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<java.util.UUID>'),
    ('java.util.Collection', 'java.lang.Iterable'),
    ('FieldDescription.InDefinedShape', 'net.bytebuddy.description.field.FieldDescription'),

    ('java.util.Collection<Map.Entry<K,V>>', 'java.util.Collection<Entry<K,V>>'),
    ('java.util.Set<Map.Entry<String,String>>', 'java.util.Set<Entry<String,String>>'),
    ('java.nio.file.Path', 'int'),


    ('org.pac4j.core.profile.CommonProfile', 'java.util.Optional<org.pac4j.core.profile.UserProfile>'),
    ('long', 'java.util.concurrent.CompletableFuture<java.lang.Long>'),
    ('java.util.BitSet', 'long'),


    ('long', 'java.lang.Number'),
    ('int', 'Position'),
    ('java.util.ArrayList<:[v0]>', 'java.util.concurrent.BlockingQueue<:[v0]>'),
    ('boolean', 'Mutable'),

    ('java.io.Closeable', 'java.lang.AutoCloseable'),
    ('rx.functions.Action1<:[v0]>', 'io.reactivex.functions.Consumer<:[v0]>'),
    ('Lucene50StoredFieldsFormat.Mode', 'Lucene87StoredFieldsFormat.Mode'),
    ('java.util.List<Pair<Integer,Integer>>', 'java.util.Map<java.lang.Integer,java.lang.Integer>'),
    ('io.netty.channel.Channel', 'io.netty.channel.ChannelHandlerContext'),
    ('java.lang.Throwable', 'java.lang.Error'),
    ('java.lang.String', 'java.lang.StringBuffer'),
    ('java.util.function.Supplier<:[v0]>', 'java.util.function.Function<java.lang.String,:[v0]>'),
    ('io.fabric8.kubernetes.client.dsl.NonNamespaceOperation<:[v0],:[v1],DoneableNamespace,Resource<Namespace,DoneableNamespace>>', 'io.fabric8.kubernetes.client.dsl.NonNamespaceOperation<:[v0],:[v1],Resource<Namespace>>'),

    ('java.math.BigInteger', 'java.math.BigDecimal'),
    ('java.util.concurrent.atomic.AtomicInteger', 'java.lang.String'),
    ('java.nio.ByteBuffer', 'java.nio.MappedByteBuffer'),
    ('java.util.Collection<:[v0]>', 'java.util.NavigableSet<:[v0]>'),
    ('java.util.function.IntConsumer', 'java.util.function.LongConsumer'),

    ('rx.functions.Func1<:[v1],:[v0]>', 'io.reactivex.functions.Function<:[v1],:[v0]>'),
    ('java.io.File', 'java.net.URL'),
    ('boolean', 'java.util.Optional<java.lang.Throwable>'),
    ('org.eclipse.jetty.server.bio.SocketConnector', 'org.eclipse.jetty.server.ServerConnector'),

    ('java.lang.Throwable', 'java.lang.OutOfMemoryError'),
    ('javax.ws.rs.core.Response', 'java.lang.String'),

    ('java.lang.AutoCloseable', 'java.io.Closeable'),
    ('org.junit.rules.Timeout', 'org.junit.rules.TestRule'),
    ('TransportResponseHandler<TcpTransportHandshaker.VersionHandshakeResponse>', 'TransportResponseHandler<TransportHandshaker.HandshakeResponse>'),
    ('TransportResponseHandler<VersionHandshakeResponse>', 'TransportResponseHandler<HandshakeResponse>'),
    ('io.netty.buffer.ByteBuf', 'io.netty.buffer.CompositeByteBuf'),

    ('java.util.concurrent.atomic.AtomicLong', 'java.util.concurrent.atomic.LongAccumulator'),
    ('java.util.Set<:[v0]>', 'java.util.TreeSet<:[v0]>'),
    ('com.google.common.collect.ImmutableSortedMap<:[v1],:[v0]>', 'com.google.common.collect.ImmutableRangeMap<:[v1],:[v0]>'),


    (':[v0]', 'java.util.HashSet<:[v0]>'),
    ('java.lang.ThreadLocal<java.lang.Integer>', 'int'),

    ('java.util.Optional<java.lang.Integer>', 'java.util.Optional<java.lang.String>'),
    ('io.atomix.Atomix', 'io.atomix.AtomixReplica'),
    ('javax.jms.Queue', 'javax.jms.Destination'),

    ('java.util.HashMap<:[v0],:[v0]>', 'java.util.Map<:[v0],:[v0]>'),
    ('org.elasticsearch.action.search.SearchRequestBuilder', 'org.elasticsearch.action.search.SearchRequest'),
    ('android.view.ViewGroup', 'android.widget.FrameLayout'),
    ('org.eclipse.jetty.server.ssl.SslSocketConnector', 'org.eclipse.jetty.util.ssl.SslContextFactory'),

    ('com.github.kristofa.brave.Brave', 'brave.Tracing'),

    ('org.apache.hadoop.security.UserGroupInformation', 'java.lang.String'),

    ('org.jboss.netty.handler.codec.embedder.DecoderEmbedder<org.jboss.netty.buffer.ChannelBuffer>', 'io.netty.channel.embedded.EmbeddedChannel'),
    ('org.nd4j.linalg.api.ndarray.INDArray', 'boolean'),
    ('HealthCheckResponse.State', 'HealthCheckResponse.Status'),

    ('AsyncRetentionLeaseBackgroundSyncTask', 'AsyncRetentionLeaseSyncTask'),
    ('groovy.lang.GroovyClassLoader', 'java.lang.ClassLoader'),

    ('org.elasticsearch.index.store.IndexStore', 'org.elasticsearch.index.IndexSettings'),
    ('com.codahale.metrics.MetricRegistry', 'io.micrometer.core.instrument.MeterRegistry'),

    ('org.pac4j.core.profile.UserProfile', 'org.pac4j.core.profile.CommonProfile'),
    ('org.pac4j.core.profile.creator.ProfileCreator<C>', 'org.pac4j.core.profile.creator.ProfileCreator'),
    ('org.pac4j.core.credentials.UsernamePasswordCredentials', 'org.pac4j.core.credentials.Credentials'),
    ('org.pac4j.jwt.profile.JwtGenerator<org.pac4j.core.profile.CommonProfile>', 'org.pac4j.jwt.profile.JwtGenerator'),
    ('org.pac4j.core.credentials.authenticator.Authenticator<C>', 'org.pac4j.core.credentials.authenticator.Authenticator'),
    ('AsyncGlobalCheckpointTask', 'AsyncTrimTranslogTask'),

    ('com.google.common.cache.LoadingCache<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentHashMap<:[v1],:[v0]>'),
    ('java.lang.Number', 'double'),
    ('java.util.Map<:[v1],:[v0]>', 'java.util.EnumMap<:[v1],:[v0]>'),


    ('org.jgroups.Message', 'org.jgroups.util.Buffer'),
    ('java.io.OutputStream', 'java.io.FileOutputStream'),
    ('java.util.Optional<java.lang.String>', 'java.util.Optional<java.net.URI>'),
    ('JCTree.JCStatement', 'com.sun.source.tree.StatementTree'),
    ('JCTree.JCCase', 'com.sun.source.tree.CaseTree'),
    ('Role', 'java.lang.String'),
    ('org.elasticsearch.action.ActionRequest', 'org.elasticsearch.action.DocWriteRequest'),
    ('java.lang.Class<T>', 'java.lang.Class<R>'),
    ('boolean', 'ColumnType'),
    ('int', 'Config'),
    ('java.util.concurrent.locks.ReentrantReadWriteLock', 'java.util.concurrent.locks.ReadWriteLock'),
    ('java.util.concurrent.CountDownLatch', 'java.lang.Runnable'),
    ('java.util.function.Consumer<org.elasticsearch.index.shard.ShardId>', 'org.elasticsearch.client.node.NodeClient'),
    ('java.io.FileReader', 'java.io.InputStream'),
    ('java.util.Map<:[v1],:[v0]>', 'ImmutableMap<:[v1],:[v0]>'),
    ('java.util.List<java.util.Map>', 'java.util.List<Map<String,String>>'),
    ('com.hazelcast.core.HazelcastInstance', 'java.lang.String'),
    ('java.util.concurrent.ExecutorCompletionService<:[v0]>', 'java.util.concurrent.CompletionService<:[v0]>'),
    ('java.io.OutputStream', 'java.io.Closeable'),




    ('java.util.concurrent.CountDownLatch', 'boolean'),
    ('long', 'java.util.UUID'),
    ('java.util.concurrent.ExecutorService', 'boolean'),
    ('Map.Entry<:[v1],:[v1]>', ':[v1]'),
    ('boolean', 'javax.ws.rs.core.Response'),
    ('java.util.List<E>', 'java.util.List<T>'),
    (':[v0]', 'java.util.Deque<:[v0]>'),
    ('com.google.common.base.Function<:[v0],java.lang.Void>', 'java.util.function.Consumer<:[v0]>'),
    ('java.nio.file.Path', 'java.io.InputStream'),
    ('Option', 'boolean'),
    ('java.util.Collection', 'java.util.List<java.lang.String>'),
    ('java.util.List<org.elasticsearch.cluster.node.DiscoveryNode>', 'java.util.List<org.elasticsearch.common.transport.TransportAddress>'),
    ('net.minecraft.util.IChatComponent', 'net.minecraft.util.text.ITextComponent'),
    ('net.minecraft.util.ChatComponentTranslation', 'net.minecraft.util.text.TextComponentTranslation'),
    ('com.zaxxer.hikari.HikariDataSource', 'javax.sql.DataSource'),
    ('long', 'java.lang.invoke.VarHandle'),
    ('boolean', 'ActivityRecord'),
    ('org.elasticsearch.cluster.ClusterChangedEvent', 'org.elasticsearch.cluster.SnapshotsInProgress'),
    ('java.lang.String', 'org.elasticsearch.cluster.metadata.IndexMetaData'),
    ('org.apache.lucene.index.IndexWriter', 'java.util.Map<java.lang.String,java.lang.String>'),
    ('java.lang.Class<E>', 'java.lang.Class<T>'),
    ('org.apache.lucene.search.SearcherManager', 'org.apache.lucene.search.ReferenceManager<org.apache.lucene.search.IndexSearcher>'),

    ('double', 'byte'),




    ('float', 'short'),
    ('short', 'float'),
    ('double', 'short'),
    ('java.lang.String', 'org.joda.time.DateTime'),

    ('java.util.List<java.lang.Integer>', 'IntArrayList'),
    ('reactor.core.publisher.FluxSink<:[v0]>', 'Sinks.Many<:[v0]>'),

    ('DataType', 'DataType<T>'),
    ('boolean', 'org.apache.hadoop.fs.FileStatus'),
    ('java.util.List<org.elasticsearch.index.engine.DocIdSeqNoAndTerm>', 'java.util.List<org.elasticsearch.index.engine.DocIdSeqNoAndSource>'),
    ('java.util.Set<AffixSetting>', 'java.util.Set<AffixSettingDependency>'),
    ('AffixSetting', 'AffixSettingDependency'),
    ('org.joda.time.DateTime', 'java.time.Instant'),
    ('java.util.List<T>', 'java.util.List<R>'),


    ('org.apache.http.StatusLine', 'int'),

    ('java.util.Optional<java.lang.Long>', 'java.util.Optional<java.lang.String>'),
    ('org.eclipse.jetty.server.Request', 'javax.servlet.http.HttpServletRequest'),
    (':[v0]', 'ConcreteReplicaRequest<:[v0]>'),
    ('org.elasticsearch.transport.TransportChannel', 'org.elasticsearch.action.ActionListener<ReplicaResponse>'),
    ('org.elasticsearch.transport.TransportChannel', 'org.elasticsearch.action.ActionListener<Response>'),
    ('android.widget.RelativeLayout', 'android.view.View'),
    ('java.util.Map.Entry<:[v1],:[v1]>', ':[v0]'),
    ('java.util.Set<java.lang.Integer>', 'java.util.BitSet'),
    ('java.io.OutputStream', 'java.lang.String'),
    ('java.util.concurrent.ExecutorService', 'java.lang.String'),

    ('org.elasticsearch.snapshots.SnapshotInfo', 'org.elasticsearch.snapshots.SnapshotId'),
    ('com.sun.source.tree.Tree', 'com.sun.source.util.TreePath'),
    ('java.util.function.Supplier<java.lang.Long>', 'java.util.function.LongSupplier'),
    ('java.util.Map<:[c6a2d4dd-31fb-3cc7-5d1b-e727ccd91482_v0_equal],:[c6a2d4dd-31fb-3cc7-5d1b-e727ccd91482_v0_equal]>', ':[v1]'),
    ('java.util.concurrent.atomic.AtomicLong', 'PrimaryTermSupplier'),
    ('java.util.SortedMap<:[v1],:[v0]>', 'java.util.TreeMap<:[v1],:[v0]>'),
    ('Snapshot', 'java.lang.String'),
    ('java.util.List<:[v0]>', 'org.elasticsearch.common.collect.ImmutableOpenMap<java.lang.String,:[v0]>'),
    ('Snapshot', 'org.elasticsearch.common.collect.ImmutableOpenMap<org.elasticsearch.index.shard.ShardId,org.elasticsearch.cluster.RestoreInProgress.ShardRestoreStatus>'),
    ('java.util.List<RestoreInProgress.Entry>', 'RestoreInProgress.Builder'),
    ('java.util.Set<Snapshot>', 'java.util.Set<java.lang.String>'),
    ('Snapshot', 'org.elasticsearch.cluster.routing.RecoverySource'),
    ('android.graphics.RectF', 'android.graphics.Rect'),

    ('Symbol.TypeSymbol', 'com.sun.tools.javac.code.Symbol'),
    ('javax.net.SocketFactory', 'javax.net.ssl.SSLSocketFactory'),
    ('java.util.Set<java.lang.String>', 'java.util.Set<VotingConfigNode>'),

    ('java.util.Collection<ChildScorer>', 'java.util.Collection<ChildScorable>'),
    ('java.util.concurrent.atomic.AtomicBoolean', 'java.util.concurrent.atomic.AtomicReference<java.lang.Throwable>'),
    ('java.lang.Thread', 'java.lang.Runnable'),
    ('org.apache.lucene.index.TermContext', 'org.apache.lucene.index.TermStates'),
    ('org.apache.lucene.search.spell.LevensteinDistance', 'org.apache.lucene.search.spell.LevenshteinDistance'),

    ('java.util.HashMap<:[v1],:[v1]>', 'java.util.Map<:[v0],:[v0]>'),
    ('java.util.concurrent.ExecutorService', 'io.netty.channel.EventLoopGroup'),
    ('java.util.concurrent.Executor', 'java.util.concurrent.ThreadPoolExecutor'),
    ('java.io.InputStream', 'java.io.Closeable'),
    ('android.widget.Button', 'android.widget.ImageButton'),
    ('long', 'MatchingNode'),
    ('com.carrotsearch.hppc.ObjectLongMap<:[v0]>', 'java.util.Map<:[v0],MatchingNode>'),


    ('com.google.common.collect.Table<:[v0],:[v2],:[v1]>', 'com.google.common.collect.ImmutableTable<:[v0],:[v2],:[v1]>'),
    ('java.util.function.Supplier<java.lang.Boolean>', 'boolean'),
    ('com.sun.source.tree.ExpressionTree', 'com.sun.source.tree.Tree'),
    ('InternalMessage', 'InternalRequest'),
    ('org.apache.lucene.store.Directory', 'org.apache.lucene.store.MMapDirectory'),
    ('org.apache.lucene.store.FSDirectory', 'org.apache.lucene.store.MMapDirectory'),
    ('java.util.Map<:[v1],:[v1]>', 'java.util.List<:[v0]>'),
    ('org.elasticsearch.transport.PlainTransportFuture<:[v0]>', 'org.elasticsearch.transport.TransportFuture<:[v0]>'),
    ('java.lang.Long', 'java.lang.Throwable'),
    ('java.util.Set<java.lang.Long>', 'java.util.Set<java.lang.Integer>'),
    ('java.util.ArrayList<:[v0]>', 'java.util.LinkedList<:[v0]>'),
    ('org.w3c.dom.Document', 'javax.xml.transform.Source'),
    ('java.util.function.Function<:[v0],java.lang.Long>', 'java.util.function.ToLongFunction<:[v0]>'),
    ('java.lang.Float', 'double'),
    ('DeleteSnapshotListener', 'org.elasticsearch.action.ActionListener<java.lang.Void>'),
    ('CreateSnapshotListener', 'org.elasticsearch.action.ActionListener<Snapshot>'),
    ('java.util.concurrent.CopyOnWriteArrayList<SnapshotCompletionListener>', 'java.util.Map<Snapshot,List<ActionListener<SnapshotInfo>>>'),
    ('SnapshotCompletionListener', 'org.elasticsearch.action.ActionListener<SnapshotInfo>'),
    ('java.util.List<org.apache.lucene.document.Field>', 'java.util.List<org.apache.lucene.index.IndexableField>'),
    ('android.widget.ImageButton', 'android.widget.ImageView'),
    ('boolean', 'java.util.OptionalLong'),
    ('io.reactivex.Observable<java.lang.Void>', 'io.reactivex.Completable'),
    ('com.google.common.collect.ImmutableList<:[v0]>', 'com.google.common.collect.ImmutableMap<:[v0],:[v0]>'),
    ('java.util.concurrent.atomic.AtomicReference<:[v0]>', 'java.util.function.Supplier<:[v0]>'),
    ('org.apache.flink.streaming.api.datastream.DataStream<:[v0]>', 'org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator<:[v0]>'),
    ('io.reactivex.Flowable<java.lang.Void>', 'io.reactivex.Completable'),
    ('java.lang.Number', 'java.lang.Double'),
    ('long', 'VersionsAndSeqNoResolver.DocIdAndVersion'),
    ('java.lang.String', 'javax.management.remote.JMXServiceURL'),
    ('long', 'SendSnapshotResult'),
    ('java.security.PublicKey', 'java.lang.String'),
    ('java.security.PrivateKey', 'java.lang.String'),
    ('short', 'java.lang.String'),
    ('org.apache.lucene.store.FileSwitchDirectory', 'FsDirectoryFactory.PreLoadMMapDirectory'),
    ('java.util.Map<:[v1],:[v1]>', 'java.util.Set<:[v0]>'),
    ('org.elasticsearch.cluster.routing.GroupShardsIterator', 'org.elasticsearch.cluster.routing.GroupShardsIterator<org.elasticsearch.cluster.routing.ShardIterator>'),
    ('io.vertx.core.Future<:[v0]>', 'io.vertx.core.Promise<:[v0]>'),
    # ('java.util.ListIterator', 'java.util.ListIterator<java.lang.String>'),
    ('org.openqa.selenium.remote.DesiredCapabilities', 'org.openqa.selenium.chrome.ChromeOptions'),
    ('java.lang.Character', 'java.lang.String'),
    # (':[v0]', 'java.util.concurrent.atomic.AtomicReference<:[v0]>'),
    # ('java.io.RandomAccessFile', 'java.nio.channels.FileChannel')
    # TypeChange("int", "long"),
    # ('java.util.HashMap<java.lang.Integer,:[v0]>', 'android.util.SparseArray<:[v0]>')
    # ("java.io.File", "java.nio.file.Path"),
    # (":[v0]", "java.util.Optional<:[v0]>"),
    # ("java.util.List<:[v0]>", "java.util.Set<:[v0]>"),
    # ('org.apache.hadoop.hbase.client.HTableInterface', 'org.apache.hadoop.hbase.client.Table')
    # ('java.util.Map<:[v1],:[v0]>', 'java.util.concurrent.ConcurrentMap<:[v1],:[v0]>')
    # ('com.mongodb.DB', 'com.mongodb.client.MongoDatabase'),
    # ('java.util.Map<:[v1],:[v0]>', 'com.google.common.cache.Cache<:[v1],:[v0]>'),
    # (":[v0]", "java.util.List<:[v0]>"),
    # TypeChange("java.lang.String", "java.util.UUID"),
    # TypeChange("java.lang.String", "java.net.URI"),
    # TypeChange("java.lang.String", "java.util.regex.Pattern"),
    # TypeChange("java.lang.String", "java.util.Set<java.lang.String>"),
    # TypeChange("java.lang.String", "java.util.File"),
    # ("java.net.URL", "java.net.URI"),
    # # ("java.net.URI", "java.net.URL"),
    # # # TypeChange("java.lang.String", "java.util.Optional<java.lang.String>"),
    # ("long", "java.time.Duration"),
    # ("long", "java.math.BigInteger"),
    # ("java.lang.Long", "java.math.BigInteger"),
    # ("int", "java.math.BigInteger"),
    # ("java.lang.Integer", "java.math.BigInteger"),
    # ("org.junit.rules.TemporaryFolder", "java.io.file"),
    # ("long", "java.time.Instant"),
    # ("java.lang.Long", "java.time.Duration"),
    # ("java.util.Date", "java.time.Instant"),
    # ("java.util.Date", "java.time.LocalDate"),
    # # TypeChange("java.util.List", "java.util.Set"),
    # TypeChange("java.util.Set", "com.google.common.collect.ImmutableSet"),
    # # TypeChange("java.util.Map", "com.google.common.collect.ImmutableMap"),
    # TypeChange("java.util.Stack", "java.util.Deque"),
    # # #
    # ("java.util.function.Function<java.lang.Double,java.lang.Double>", "java.util.function.DoubleUnaryOperator"),
    # ("java.util.function.Function<java.lang.Integer,java.lang.Integer>", "java.util.function.IntUnaryOperator"),
    # ("java.util.function.Function<java.lang.Long,java.lang.Long>", "java.util.function.LongUnaryOperator"),
    # ("java.util.function.Function", "java.util.function.IntPredicate"),
    # ("java.util.function.Predicate", "java.util.function.IntPredicate"),
    # ("java.util.function.Function", "java.util.function.Predicate"),
    # ("java.util.function.Function", "java.util.function.LongPredicate"),
    # ("java.util.function.Predicate", "java.util.function.LongPredicate"),
    # TypeChange("java.util.Optional<java.lang.Integer>", "java.util.OptionalInt"),
    # ("java.text.SimpleDateFormat", "java.time.format.DateTimeFormatter"),
    # TypeChange("java.util.Map<java.lang.String, java.lang.String>", "java.util.Properties"),
    # TypeChange("org.json.JSONObject", "com.google.gson.JsonObject"),
    # TypeChange("java.util.concurrent.Callable", "java.util.function.Supplier"),
    # TypeChange("java.util.function.Function", "java.util.function.ToDoubleFunction"),
    # ('org.apache.flink.runtime.concurrent.Future<:[v0]>', 'java.util.concurrent.CompletableFuture<:[v0]>'),
    # TypeChange("java.util.function.Function", "java.util.function.ToIntFunction"),
    # ('org.apache.commons.logging.Log', 'org.slf4j.Logger'),
    # TypeChange("java.util.function.Function", "java.util.function.ToLongFunction"),
    # TypeChange("java.util.function.Function", "java.util.function.Predicate"),
    # TypeChange("java.util.function.Function", "java.util.function.IntPredicate"),
    # TypeChange("java.util.function.Predicate", "java.util.function.IntPredicate"),
    # TypeChange("java.util.Optional<java.lang.Integer>", "java.util.OptionalInt"),
    # TypeChange("long", "java.util.concurrent.atomic.AtomicLong"),
    # TypeChange("int", "java.util.concurrent.atomic.AtomicInteger"),
    # TypeChange("java.util.Map<:[v0],:[v1]>", "java.util.concurrent.ConcurrentMap<:[v0],:[v1]>"),
    # TypeChange("java.util.concurrent.BlockingQueue", "java.util.Queue"),
    # TypeChange("org.apache.hadoop.hbase.KeyValue", "org.apache.hadoop.hbase.Cell")
]

generate_input()
