# BuildRight v1.0.1 Input Fix

This build fixes the non-working/editable-field problem in the first unified UI.

Working editable inputs now include:
- project name
- width, length and wall height
- designer geometry and window count
- door/window wall labels
- stud, joist and rafter spacing
- roof pitch and waste percentage
- foundation and roof-style text
- add/edit/delete materials
- quantity, unit and unit price
- purchased checkboxes
- field/build task entry
- manual material pricing
- country/state/county/city jurisdiction fields
- notes
- assistant text entry

Changes are written through ProjectStore using the existing onChanged callback. Local Compose state is used so typed text updates immediately even though the legacy Project model uses ordinary mutable Kotlin properties.
