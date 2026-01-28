# Dynamic Sync Widgets

!!! Note
    This method is obsolete as of MUI 3.1, and the [Dynamic Linked Sync Handler](Dynamic-Linked-Sync-Handler.md) should be used in its place.


```java
public class MuiTestMachine extends MetaMachine implements IMuiMachine {
    
    public int number = 0;
    public MuiTestMachine(BlockEntityCreationInfo info) {
        super(info);
        this.subscribeServerTick(() -> {
            number = (number + 1) % 10;
        });
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        var panel = GTGuis.createPanel(this, 176, 168);

        var numberSyncValue = new IntSyncValue(() -> this.number, (newValue) -> this.number = newValue);
        syncManager.syncValue("number", numberSyncValue);


        DynamicSyncHandler numberListWidgetHandler = new DynamicSyncHandler().widgetProvider((slotsSyncManger, buffer) -> {
            int number = buffer.readInt();
            Flow numberList = new Column().width(100);
            for (int i=0; i<number; i++) {
                final int finalI = i;
                numberList.child(IKey.dynamic(() -> Component.literal("Number: " + finalI)).asWidget().width(100));
            }
            return numberList;
        });

        numberSyncValue.setChangeListener(() -> {
            numberListWidgetHandler.notifyUpdate(buffer -> {
                buffer.writeInt(number);
            });
        });

        panel.child(new DynamicSyncedWidget<>().syncHandler(numberListWidgetHandler).margin(4));
        return panel;
    }
}
```

DynamicSyncWidgets are required when the structure or more complex properties of your widget change based off of your data.

The example buildUI() method above consists of approximately four parts:  

1. Registering a sync value for the number variable.  
2. Creating a DynamicSyncHandler that essentially creates a Column with numbers 0 to i in the list, with the value from step 3.  
3. Setting a change listener on the number sync value, to refresh (notify) the numberListWidget and write the number variable to the buffer for use in step 2.  
4. Creating the actual widget and attaching it to the panel.  

Note that we're not relying on the number sync value to actually sync the number, in this case we're only using it to notify and update our dynamic widget.

The `changeListener` for the `numberSyncValue` calls notifyUpdate, which writes the number to a buffer and sends that data to the client. The client then takes that packet, applies it to our `DynamicSyndHandler` and updates the UI to include the new version of the widget.