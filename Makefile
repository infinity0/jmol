PRIVATE_FILE = none
JSV_FLAGS += -DPrivate.propertyFile=$(PRIVATE_FILE)
JMOL_FLAGS += -DPrivate.propertyFile=$(PRIVATE_FILE)

build:
# JSV
	cd JSpecViewLib && ant clean && ant # "ant clean" actually copies files from Jmol
	cd JSpecView && ant $(JSV_FLAGS) make-application-jar
# Jmol, part 1
	mkdir -p Jmol/build
	cd Jmol && ant $(JMOL_FLAGS) main
# JSmol
# Note: we *must* supply Eclipse with -data $$PWD otherwise this build will
# interfere with other workspaces and has a high chance of failing.
	cd JSmol && ant -f build_11_fromjmol.xml
	cd JSmol && ant -f build_12_fromjspecview.xml
	eclipse -consoleLog -clean -debug -data $$PWD \
	  -nosplash -application net.sf.j2s.ui.cmdlineApi \
	  -cmd build -path $$PWD/JSmol
	cd JSmol && ant -Djmol.path=../Jmol -f build_13_tojs.xml
# Jmol, part 2
	cd Jmol && ant $(JMOL_FLAGS) all

test:
	cd Jmol && ant $(JMOL_FLAGS) test

clean:
# JSmol
	rm -rf JSmol/bin JSmol/dist/jsmol.zip JSmol/jnlp/JmolApplet*.jar
	rm -rf JSmol/site JSmol/src JSmol/srcjs/js
	rm -rf $$PWD/.metadata # clean eclipse workspace
# Jmol
	mkdir -p Jmol/build
	cd Jmol && ant $(JMOL_FLAGS) clean clean-after-dist
	rm -rf Jmol/build Jmol/appletweb/jsmol.zip Jmol/jars/JSpecView.jar
	rm -rf Jmol/src/org/jmol/translation/translations.tgz Jmol/Jmol.properties
# JSV
	cd JSpecView && ant $(JSV_FLAGS) clean
	rm -rf JSpecViewLib/src/javajs JSpecViewLib/src/org
	rm -rf JSpecView/bin JSpecView/build

.PHONY: configure build test clean
