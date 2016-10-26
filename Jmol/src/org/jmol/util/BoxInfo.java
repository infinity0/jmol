/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.util;


import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.T3;
import javajs.util.V3;

/**
 * The BoxInfo class holds critical information about boundboxes. 
 * These are simple tetragonal spaces lined up with x,y,z.
 * 
 */
public class BoxInfo {
 
  public final P3 bbCorner0 = new P3();
  public final P3 bbCorner1 = new P3();
  private final P3 bbCenter = new P3();
  private final V3 bbVector = new V3();
  
  /**
   * The ordering of these vertices is given below. Do not mess with that.
   * 
   */
  private final Point3fi[] bbVertices = new Point3fi[8];
  private boolean isScaleSet;
  private float margin;

  public static char[] bbcageTickEdges = {
    'z', '\0', '\0', 'y', 
    'x', '\0', '\0', '\0', 
    '\0', '\0', '\0', '\0'};
  
  public static char[] uccageTickEdges = {
    'z', 'y', 'x', '\0', 
    '\0', '\0', '\0', '\0', 
    '\0', '\0', '\0', '\0'};
  
  public final static byte edges[] = {
    0,1, 0,2, 0,4, 1,3, 
    1,5, 2,3, 2,6, 3,7, 
    4,5, 4,6, 5,7, 6,7
  };

  public BoxInfo() {
    for (int i = 8; --i >= 0;)
      bbVertices[i] = new Point3fi();
    reset();
  }
  
  public void reset() {
    isScaleSet = false;
    bbCorner0.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    bbCorner1.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
  }
  
  public P3[] getMyCanonicalCopy(float scale) {
    return getCanonicalCopy(bbVertices, scale);
  }

  public final static P3[] getCanonicalCopy(P3[] bbUcPoints, float scale) {
    P3[] pts = new P3[8];
    for (int i = 0; i < 8; i++)
      pts[toCanonical[i]] = P3.newP(bbUcPoints[i]);
    scaleBox(pts, scale);
    return pts;
  }
  
  public static void scaleBox(P3[] pts, float scale) {
    if (scale == 0 || scale == 1)
      return;
    P3 center = new P3();
    V3 v = new V3();
    for (int i = 0; i < 8; i++)
      center.add(pts[i]);
    center.scale(1/8f);
    for (int i = 0; i < 8; i++) {
      v.sub2(pts[i], center);
      v.scale(scale);
      pts[i].add2(center, v);
    }
  }

  // note that this box is NOT the same as for Marching Cubes
  //
  //                     Y 
  //                      2 --------6--------- 6                            
  //                     /|                   /|          
  //                    / |                  / |           
  //                   /  |                 /  |           
  //                  5   1               11   |           
  //                 /    |               /    9           
  //                /     |              /     |         
  //               3 --------7--------- 7      |         
  //               |      |             |      |         
  //               |      0 ---------2--|----- 4    X        
  //               |     /              |     /          
  //               3    /              10    /           
  //               |   0                |   8            
  //               |  /                 |  /             
  //               | /                  | /               
  //               1 ---------4-------- 5                 
  //              Z                                       
  
  public final static P3[] unitCubePoints = { 
    P3.new3(0, 0, 0), // 0
    P3.new3(0, 0, 1), // 1
    P3.new3(0, 1, 0), // 2
    P3.new3(0, 1, 1), // 3
    P3.new3(1, 0, 0), // 4
    P3.new3(1, 0, 1), // 5
    P3.new3(1, 1, 0), // 6
    P3.new3(1, 1, 1), // 7
  };

  public final static P3i[] facePoints = {
    P3i.new3(4, 0, 6),
    P3i.new3(4, 6, 5), 
    P3i.new3(5, 7, 1), 
    P3i.new3(1, 3, 0),
    P3i.new3(6, 2, 7), 
    P3i.new3(1, 0, 5), 
    
    P3i.new3(0, 2, 6),
    P3i.new3(6, 7, 5), 
    P3i.new3(7, 3, 1), 
    P3i.new3(3, 2, 0),
    P3i.new3(2, 3, 7), 
    P3i.new3(0, 4, 5), 
  };

  public final static int[] toCanonical = new int[] {0, 3, 4, 7, 1, 2, 5, 6};

  protected final static P3i[] cubeVertexOffsets = { 
    P3i.new3(0, 0, 0), //0 pt
    P3i.new3(1, 0, 0), //1 pt + yz
    P3i.new3(1, 0, 1), //2 pt + yz + 1
    P3i.new3(0, 0, 1), //3 pt + 1
    P3i.new3(0, 1, 0), //4 pt + z
    P3i.new3(1, 1, 0), //5 pt + yz + z
    P3i.new3(1, 1, 1), //6 pt + yz + z + 1
    P3i.new3(0, 1, 1)  //7 pt + z + 1 
  };

  /**
   * Delivers [center a b c] for generation of unit cells from a boundbox
   * 
   * @param bbVertices
   * @param offset
   * @return [center a b c]
   */
  public final static P3[] getUnitCellPoints(P3[] bbVertices, T3 offset) {
    P3 center = P3.newP(bbVertices[0]);
    P3 a = P3.newP(bbVertices[1]);
    P3 b = P3.newP(bbVertices[2]);
    P3 c = P3.newP(bbVertices[4]);
    a.sub(center);
    b.sub(center);
    c.sub(center);
    if (offset != null)
      center.add(offset);
    return new P3[] { center, a, b, c };
  }
  
  private final static P3[] unitBboxPoints = new P3[8];
  {
    for (int i = 0; i < 8; i++) {
      unitBboxPoints[i] = P3.new3(-1, -1, -1);
      unitBboxPoints[i].scaleAdd2(2, unitCubePoints[i], unitBboxPoints[i]);
    }
  }

  public P3 getBoundBoxCenter() {
    if (!isScaleSet)
      setBbcage(1);
    return bbCenter;
  }

  public V3 getBoundBoxCornerVector() {
    if (!isScaleSet)
      setBbcage(1);
    return bbVector;
  }

  /**
   * Return basic info on boundbox in the form of an array.
   *  
   * @param isAll to include center and diagonal
   * @return isAll: [(0.5 0.5 0.5), diagonal, (0 0 0), (1 1 1)], otherwise just [(0 0 0), (1 1 1)]
   * 
   */
  public P3[] getBoundBoxPoints(boolean isAll) {
    if (!isScaleSet)
      setBbcage(1);
    return (isAll ? new P3[] { bbCenter, P3.newP(bbVector), bbCorner0,
        bbCorner1 } : new P3[] { bbCorner0, bbCorner1 });
  }

  public Point3fi[] getBoundBoxVertices() {
    if (!isScaleSet)
      setBbcage(1);
    return bbVertices;
  }
  
  public void setBoundBoxFromCriticalPoints(T3[] points) {
    P3 origin = P3.newP(points[0]);
    P3 pt111 = new P3();
    for (int i = 0; i < 4; i++)
      pt111.add(points[i]);
    setBoundBox(origin, pt111, true, 1);
  }
  
  public void setBoundBox(T3 pt1, T3 pt2, boolean byCorner, float scale) {
    if (pt1 != null) {
      if (scale == 0)
        return;
      if (byCorner) {
        if (pt1.distance(pt2) == 0)
          return;
        bbCorner0.set(Math.min(pt1.x, pt2.x), Math.min(pt1.y, pt2.y), Math.min(
            pt1.z, pt2.z));
        bbCorner1.set(Math.max(pt1.x, pt2.x), Math.max(pt1.y, pt2.y), Math.max(
            pt1.z, pt2.z));
      } else { // center and vector
        if (pt2.x == 0 || pt2.y == 0 && pt2.z == 0)
          return;
        bbCorner0.set(pt1.x - pt2.x, pt1.y - pt2.y, pt1.z - pt2.z);
        bbCorner1.set(pt1.x + pt2.x, pt1.y + pt2.y, pt1.z + pt2.z);
      }
    }
    setBbcage(scale);
  }

  public void setMargin(float m) {
    margin = m;
  }
  
  public void addBoundBoxPoint(T3 pt) {
    isScaleSet = false;
    addPoint(pt, bbCorner0, bbCorner1, margin);
  }

  public static void addPoint(T3 pt, T3 xyzMin, T3 xyzMax, float margin) {
    if (pt.x - margin < xyzMin.x)
      xyzMin.x = pt.x - margin;
    if (pt.x + margin > xyzMax.x)
      xyzMax.x = pt.x + margin;
    if (pt.y - margin < xyzMin.y)
      xyzMin.y = pt.y - margin;
    if (pt.y + margin > xyzMax.y)
      xyzMax.y = pt.y + margin;
    if (pt.z - margin < xyzMin.z)
      xyzMin.z = pt.z - margin;
    if (pt.z + margin > xyzMax.z)
      xyzMax.z = pt.z + margin;
  }
 
  public static void addPointXYZ(float x, float y, float z, P3 xyzMin, P3 xyzMax, float margin) {
    if (x - margin < xyzMin.x)
      xyzMin.x = x - margin;
    if (x + margin > xyzMax.x)
      xyzMax.x = x + margin;
    if (y - margin < xyzMin.y)
      xyzMin.y = y - margin;
    if (y + margin > xyzMax.y)
      xyzMax.y = y + margin;
    if (z - margin < xyzMin.z)
      xyzMin.z = z - margin;
    if (z + margin > xyzMax.z)
      xyzMax.z = z + margin;
  }

  public void setBbcage(float scale) {
    isScaleSet = true;
    bbCenter.add2(bbCorner0, bbCorner1);
    bbCenter.scale(0.5f);
    bbVector.sub2(bbCorner1, bbCenter);
    if (scale > 0) {
      bbVector.scale(scale);
    } else {
      bbVector.x -= scale / 2;
      bbVector.y -= scale / 2;
      bbVector.z -= scale / 2;
    }
    for (int i = 8; --i >= 0;) {
      P3 pt = bbVertices[i];
      pt.setT(unitBboxPoints[i]);
      pt.x *= bbVector.x;
      pt.y *= bbVector.y;
      pt.z *= bbVector.z;
      pt.add(bbCenter);
    }
    bbCorner0.setT(bbVertices[0]);
    bbCorner1.setT(bbVertices[7]);
  }
  
  public boolean isWithin(P3 pt) {
    if (!isScaleSet)
      setBbcage(1);
   return (pt.x >= bbCorner0.x && pt.x <= bbCorner1.x 
       && pt.y >= bbCorner0.y && pt.y <= bbCorner1.y
       && pt.z >= bbCorner0.z && pt.z <= bbCorner1.z); 
  }

  public float getMaxDim() {
    return bbVector.length() * 2;
  }

}
