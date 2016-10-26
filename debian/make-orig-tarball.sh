#!/bin/sh
# Fully automated Jmol source distribution tarball, that adheres to FOSS and
# general software-engineering best practise.
#
# THIS SCRIPT IS CURRENTLY INCOMPLETE. TODO ITEMS ARE:
# 1. Add commands to generate JSpecView/doc to the Makefile below.
# 2. Remove JSmol/j2s/java and add commands to regenerate it, to said Makefile.
# 
set -e
VERSION="14.6.4_2016.10.23_INCOMPLETE-PREALPHA"

ignore_and_remove() {
	eval rm -rf "$1"
	# Add what we just ignored to .gitignore. This is just for demonstration
	# purposes - you should set the equivalent svn:ignore property in your SVN.
	echo "/$1" >> .gitignore
}

pkgdir="jmol_${VERSION}_full-src"
rm -rf "$pkgdir"
mkdir -p "$pkgdir"

echo >&2 "Building directory structure in: $pkgdir"
# Work inside that directory
set -x
( cd "$pkgdir"

################################################################################
# Check out source code from SVN
################################################################################

# These SVN revision numbers need to be updated manually for each new release.
# It is easier for the upstream developers (you) to do this, than for distro
# packagers to try to reverse-engineer this information for every release.
svn checkout svn://svn.code.sf.net/p/jmol/code/branches/v14_6/Jmol@21277
svn checkout svn://svn.code.sf.net/p/jspecview/svn/dev2/JSpecView@1735
svn checkout svn://svn.code.sf.net/p/jspecview/svn/dev2/JSpecViewLib@1735
svn checkout svn://svn.code.sf.net/p/jsmol/code/trunk@926 JSmol

# Remove SVN directories
rm -rf */.svn

# Now we remove other stuff so the working directory is clean and adheres to
# engineering best practises for source code releases. This clean state needs
# to be maintained across future releases of Jmol. The easiest way to do this,
# is to simply remove the following files from SVN, so that this script doesn't
# have to be updated with the correct things to remove, every single release.
# More detailed discussion is further below.

################################################################################
# Remove auto-generated files
################################################################################

# Normally, someone who downloads a source tarball *wants to* regenerate these
# themselves. So convention is to omit these for FOSS source code releases.
# This also makes your own life easier, and reduces your own development costs!
#
# If you want to keep previously-built stuff, you could do this in a separate
# SVN repository away from the actual source code. This makes it easier for
# new contributors and users, to figure out which files are "source code" and
# which files are generated or copied from other places. Yes, you do document
# this in some text files, but this can get out-of-date, and raises the
# development cost for *you*. If you moved them to a different SVN repo, you
# would no longer have to expend this cost.

echo "# Ignore auto-generated files" >> .gitignore
ignore_and_remove 'Jmol/appletweb/*.zip'
ignore_and_remove Jmol/jars/JSpecView.jar
ignore_and_remove Jmol/srcjsv/jspecview/
ignore_and_remove 'JSmol/dist/*.zip'
ignore_and_remove JSmol/jars/JSpecView.jar
ignore_and_remove JSpecView/build/
ignore_and_remove 'JSpecView/dist/*.zip'
ignore_and_remove JSpecView/doc/
ignore_and_remove JSpecViewLib/src/javajs/
ignore_and_remove JSpecViewLib/src/org/jmol/

ignore_and_remove JSmol/srcjs/javajs/
# The above removal requires the below patch to work.
# You made this change to "break" a circular dependency but it is better to
# revert it here. Besides, this is not the proper way to break circular
# dependencies - copying code is not *really* breaking a circular dependency,
# it simply hides it in a very bad way. This wastes future development time.
# The proper way to break circular dependencies is to split these class files
# away into another project "Jmol-libs", that doesn't depend on anything else.
sed -i -e 's/\r$//g' JSmol/build_11_fromjmol.xml # convert CRLF to LF
patch -l -p1 <<'eof'
--- a/JSmol/build_11_fromjmol.xml
+++ b/JSmol/build_11_fromjmol.xml
@@ -132,14 +132,12 @@
       </fileset>
     </copy>
 
-  	<!--
   	<echo>copying all files from ${jmol.path}/src/javajs</echo>
     <copy overwrite="true" todir="src" >
       <fileset dir="${jmol.path}/src">
         <include name="javajs/**/*" />
       </fileset>
     </copy>
--->
   	
   	<!-- copy only the essential files, excluding all classes referencing AWT and SWING -->
   	<echo>copying selected files from ${jmol.path}/src/org/jmol</echo>
eof
sed -i -e 's/$/\r/g' JSmol/build_11_fromjmol.xml # convert LF back to CRLF

#ignore_and_remove JSmol/lib/jsme/jsme/
# According the the principles I outlined above, we really should remove JSME.
# However as a temporary caveat, it is kept for now because (1) the authors
# don't publish source code and (2) even if they did, I am unlikely to be able
# to test a GWT build soon. I *will* completely remove it in the Debian version
# of JSmol, and in the future this situation should be resolved properly.

################################################################################
# Remove unused and obsolete files
################################################################################

# These files don't seem to be used, but were used in an older version. We
# don't want them in the source tarball since they waste time for new reviewers
# who have to spend time figuring out that they're not used.
#
# It would also be good to delete them from the current SVN revision. If you
# need them in the future, just check them out again from a previous revision.

rm -rf Jmol/appletweb/old/
rm -rf Jmol/packaging/
rm -rf Jmol/plugin-jars/
rm -rf Jmol/src/com/sparshui/gestures/unused_gestures.zip
rm -rf Jmol/unused/
rm -rf JSmol/jars/Acme.jar
rm -rf JSmol/jars/commons-cli-1.0.jar
rm -rf JSmol/jars/gnujaxp.jar
rm -rf JSmol/jars/gnujaxp-onlysax.jar
rm -rf JSmol/jars/itext-1.4.5.jar
rm -rf JSmol/jars/naga-2_1-r42.jar
rm -rf JSmol/lib/jsme0/
rm -rf JSmol/lib/jme/
rm -rf JSmol/old/
rm -rf JSmol/unused/
rm -rf JSpecView/unused/

# This is in the wrong place, Jmol needs it not JSmol
mv JSmol/jars/saxon.jar Jmol/jars/saxon.jar

################################################################################
# Add a Makefile
################################################################################

# This is a script which will automatically build everything.
# It recreates the instructions in Jmol/build.README.txt, but fully-automated.
# This allows for much larger-scale engineering than clicking on different
# buttons in Eclipse many many times manually.
cat >Makefile <<'eof'
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
eof

) # exit subshell, we go back to the original directory

echo >&2 "Creating tarball: ${pkgdir}.tar.xz"
# Tar it up
tar cJf "${pkgdir}.tar.xz" "$pkgdir"
