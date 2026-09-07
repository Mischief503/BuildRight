# BuildRight 1.0.4b test compilation fix

The Kotlin 2.2.20 toolchain fix worked: GitHub compiled the production Android
source successfully. The next failure was only in `BuildEngineTest.kt`, which
referenced `result.floorArea`, a property that does not exist on the current
Build Engine result model.

The test now references the actual public area property exposed by the engine.
No production calculation was changed by this patch.
