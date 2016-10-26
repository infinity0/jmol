developers:

INSTALLING JAVA2SCRIPT

At this point in time, you will need either Eclipse 3.7, 3.8, 4.2, 4.3, or 4.4 
for the Java2Script compiler to work. The plug-in can be found in 
j2s/eclipse_plugin. 

Select the appropriate j2s.core version and place it in your eclipse/plugins/ directory.

Bob Hanson 10/23/2016

BUILDING THE PROJECT

All of the code in src is derived from either the Jmol project or the JSpecView project. 
Oddly enough, this directory is temporary only; it is in svn-ignore, and it is deleted when 
build_xx_fromjmol.xml is run.

Note that the differences between 01/11, 02/12, 03/13 are very minor -- just 
setting the source directories or comments. 

See the detailed notes in build_01_fromjmol.xml.

Please make commit comments in Jmol/src/viewer/Jmol.properties, not here.

You may have to right-click on build_01_tojs.xml and set its 
external configuration to:

-- refresh resources upon completion (project...)
-- build project before starting

Also, you should turn off "project -- build Automatically", because it
seems to slow things down.


hansonr@stolaf.edu 8/17/2013, 10/1/2013, 10/24/2016


