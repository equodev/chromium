## Workspace configurations

Once you open a new workspace in Eclipse (the IDE installed before), there are a few settings needed to work well with the Framework:

1. Configure compilance compatibility to generate _.class_ files for Java 1.8 (Window -> Preferences -> Java -> Compiler).
2. Import the _checkstyle_ file to check Equo code styling as you are developing. For this, you need to:
   1. Go to `Window -> Preference -> Checkstyle`.
   2. Select `New...` in the _Global Check Configurations_ section.
   3. Change the type of the new configuration to `External Configuration File`:
      - Write "Equo" on the name field.
      - Import from this project the file located in `config/checkstyle/checkstyle-eclipse.xml` in the _location_ field.
   4. Press Ok
   5. Select the recently created configuration and press the `Set as Default` button.
   6. Check `Run Checkstyle in backgroun on full builds` option.
3. Import Equo formatter for Eclipse:
   1. Go to `Window -> Preference -> Java -> Code Style -> Formatter`.
   2. Press `Import...` and select the file of this project located in `config/eclipse/Equo formatter.xml`.
   3. Apply changes.
