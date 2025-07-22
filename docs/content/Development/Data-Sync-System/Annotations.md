---
title: "Annotations"
---

# Annotations
The following annotations define the sync/save behaviour for an `ISyncManaged` object.

### `@SaveField`

The `@SaveField` annotation defines a field that should be saved to the server. `nbtKey` is optional, the key will default to the field name.
```java
@SaveField(nbtKey="nbtKeyToSaveTo")
public int mySyncInt = 10;
```

### `@SyncToClient`

The `@SyncToClient` annotation defines a field with a value that should be synced to clients.

Annotating a `@SyncToClient` field with `@RerenderOnChanged` will cause clients to rerender the block entity when this field changes.

!!! warning "Client sync fields **do not** automatically detect changes. When changing a client sync field, call `ISyncManaged.getSyncDataHolder().markClientSyncFieldDirty(FIELD_NAME)"
```java
@SaveField(nbtKey="nbtKeyToSaveTo")
@SyncToClient
public int mySaveAndSyncInt = 10;

@SyncToClient
@RerenderOnChanged
public long mySyncRerenderLong = 10000L;
```

### `@ClientFieldChangeListener`

The `@ClientFieldChangeListener` annotation defines a method to be called on the client when a client sync field has changed value;
```java
@SyncToClient
public long mySyncCallbackLong = 10000L;

@ClientFieldChangeListener(fieldName="mySyncCallbackLong")
public void syncLongHasChanged() {
    
}
```

### `@FieldDataModifier` and `@CustomDataField`

The `@FieldDataModifier`annotation defines custom processing to be performed on the NBT for a field, e.g. for compatibility reasons.

The `@CustomDataField`annotation defines a field with a type too complex to be serialised using the normal system. Custom data fields must have exactly one load modifier and one data modifier.

Field data modifiers on non-custom fields will be applied *after* standard serialisation/deserialisation.

```java

@CustomDataField
public VeryComplexType myVeryComplexValue = new VeryComplexType();

@FieldDataModifier(fieldName="myVeryComplexValue", target=FieldDataModifier.MODIFY_TARGET.LOAD_NBT)
public void loadVeryComplexValue(CompoundTag tag) {
    // Process tag here.
    myVeryComplexValue = new VeryComplexType();
}

@FieldDataModifier(fieldName="myVeryComplexValue", target=FieldDataModifier.MODIFY_TARGET.LOAD_NBT)
public Tag saveVeryComplexValue() {
    // Create tag here.
    return new CompoundTag();
}


```