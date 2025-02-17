---
title: Element Creation
---


## Element Creation

Elements are the base of GT materials. Registering an element WILL NOT add any items.
To make a new element(NOTE: you can add only elements that are NOT present on the periodic table),
write an `event.create()` call in the registry function like in the example below.

The following parameters are available for elements:

1. Proton Count (use -1 if it is not an element that will get a material)
2. Neutron Count (use -1 if it is not an element that will get a material)
3. Atomic Symbol (displayed in chemical formulas)
4. Translatable Name (custom language key to be used for the material)
5. Half Life Seconds (only for elements that should decay to another element)
6. Material to decay to (only for elements that should decay to another element)
7. Is isotope (ex. Uranium 235 and Uranium 238)

When a material will be created from this element, the above properties will affect the auto-generated recipes.

```js
GTCEuStartupEvents.registry('gtceu:element', event => {
   event.create('test_element')
      .protons(27) // Required
      .neutrons(177) // Required
      .symbol('test') // Required
      .translatableName('my.custom.language.key')
      .halfLifeSeconds(60)
      .decayTo('another_element')
      .isIsotope(true)
})
```
