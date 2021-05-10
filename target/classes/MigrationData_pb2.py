# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: MigrationData.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import Models.Models.TypeGraph_pb2 as TypeGraph__pb2
import Models.Models.NameSpace_pb2 as NameSpace__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='MigrationData.proto',
  package='',
  syntax='proto3',
  serialized_options=None,
  serialized_pb=b'\n\x13MigrationData.proto\x1a\x0fTypeGraph.proto\x1a\x0fNameSpace.proto\"\xc3\x01\n\rMigrationData\x12\x18\n\x04type\x18\x01 \x01(\x0b\x32\n.TypeGraph\x12\r\n\x05ratio\x18\x02 \x01(\x01\x12\x1d\n\tnamespace\x18\x03 \x01(\x0e\x32\n.NameSpace\x12\x31\n\x0c\x63ommitToType\x18\x04 \x03(\x0b\x32\x1b.MigrationData.CommitToType\x1a\x37\n\x0c\x43ommitToType\x12\x0b\n\x03sha\x18\x02 \x01(\t\x12\x1a\n\x06toType\x18\x01 \x03(\x0b\x32\n.TypeGraphb\x06proto3'
  ,
  dependencies=[TypeGraph__pb2.DESCRIPTOR,NameSpace__pb2.DESCRIPTOR,])




_MIGRATIONDATA_COMMITTOTYPE = _descriptor.Descriptor(
  name='CommitToType',
  full_name='MigrationData.CommitToType',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='sha', full_name='MigrationData.CommitToType.sha', index=0,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='toType', full_name='MigrationData.CommitToType.toType', index=1,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=198,
  serialized_end=253,
)

_MIGRATIONDATA = _descriptor.Descriptor(
  name='MigrationData',
  full_name='MigrationData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='type', full_name='MigrationData.type', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ratio', full_name='MigrationData.ratio', index=1,
      number=2, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='namespace', full_name='MigrationData.namespace', index=2,
      number=3, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='commitToType', full_name='MigrationData.commitToType', index=3,
      number=4, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_MIGRATIONDATA_COMMITTOTYPE, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=58,
  serialized_end=253,
)

_MIGRATIONDATA_COMMITTOTYPE.fields_by_name['toType'].message_type = TypeGraph__pb2._TYPEGRAPH
_MIGRATIONDATA_COMMITTOTYPE.containing_type = _MIGRATIONDATA
_MIGRATIONDATA.fields_by_name['type'].message_type = TypeGraph__pb2._TYPEGRAPH
_MIGRATIONDATA.fields_by_name['namespace'].enum_type = NameSpace__pb2._NAMESPACE
_MIGRATIONDATA.fields_by_name['commitToType'].message_type = _MIGRATIONDATA_COMMITTOTYPE
DESCRIPTOR.message_types_by_name['MigrationData'] = _MIGRATIONDATA
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

MigrationData = _reflection.GeneratedProtocolMessageType('MigrationData', (_message.Message,), {

  'CommitToType' : _reflection.GeneratedProtocolMessageType('CommitToType', (_message.Message,), {
    'DESCRIPTOR' : _MIGRATIONDATA_COMMITTOTYPE,
    '__module__' : 'MigrationData_pb2'
    # @@protoc_insertion_point(class_scope:MigrationData.CommitToType)
    })
  ,
  'DESCRIPTOR' : _MIGRATIONDATA,
  '__module__' : 'MigrationData_pb2'
  # @@protoc_insertion_point(class_scope:MigrationData)
  })
_sym_db.RegisterMessage(MigrationData)
_sym_db.RegisterMessage(MigrationData.CommitToType)


# @@protoc_insertion_point(module_scope)