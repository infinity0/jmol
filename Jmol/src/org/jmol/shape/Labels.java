/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-08-12 14:21:41 +0200 (Fri, 12 Aug 2016) $
 * $Revision: 21211 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shape;

import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Text;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;

import javajs.awt.Font;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;

import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;

import java.util.Hashtable;


import java.util.Map;

public class Labels extends AtomShape {

  public String[] strings;
  public String[] formats;
  public short[] bgcolixes;
  public byte[] fids;
  public int[] offsets;

  private Map<Integer, Text> atomLabels = new Hashtable<Integer, Text>();
  private Map<Integer, float[]> labelBoxes;

  public BS bsFontSet;
  public BS bsBgColixSet;

  public int defaultOffset;
  public int defaultAlignment;
  public int defaultZPos;
  public byte defaultFontId;
  public short defaultColix;
  public short defaultBgcolix;
  public byte defaultPaletteID;
  public int defaultPointer;
  public byte zeroFontId;

  private boolean defaultsOnlyForNone = true;
  private boolean setDefaults = false;
  
  //labels

  @Override
  public void initShape() {
    defaultFontId = zeroFontId = vwr.gdata.getFont3DFSS(JC.DEFAULT_FONTFACE,
        JC.DEFAULT_FONTSTYLE, JC.LABEL_DEFAULT_FONTSIZE).fid;
    defaultColix = 0; //"none" -- inherit from atom
    defaultBgcolix = 0; //"none" -- off
    defaultOffset = JC.LABEL_DEFAULT_OFFSET;
    defaultAlignment = JC.TEXT_ALIGN_LEFT;
    defaultPointer = JC.LABEL_POINTER_NONE;
    defaultZPos = 0;
    translucentAllowed = false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    isActive = true;

    if ("setDefaults" == propertyName) {
      setDefaults = ((Boolean) value).booleanValue();
      return;
    }

    if ("color" == propertyName) {
      byte pid = PAL.pidOf(value);
      short colix = C.getColixO(value);
      if (!setDefaults) {
        int n = checkColixLength(colix, bsSelected.length());
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < n; i = bsSelected
            .nextSetBit(i + 1))
          setLabelColix(i, colix, pid);
      }
      if (setDefaults || !defaultsOnlyForNone) {
        defaultColix = colix;
        defaultPaletteID = pid;
      }
      return;
    }

    if ("scalereference" == propertyName) {
      if (strings == null)
        return;
      float val = ((Float) value).floatValue();
      float scalePixelsPerMicron = (val == 0 ? 0 : 10000f / val);
      int n = Math.min(ac, strings.length);
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < n; i = bsSelected
          .nextSetBit(i + 1)) {
        Text text = getLabel(i);
        if (text == null) {
          text = Text.newLabel(vwr, null, strings[i], C.INHERIT_ALL, (short) 0,
              0, scalePixelsPerMicron);
          putLabel(i, text);
        } else {
          text.setScalePixelsPerMicron(scalePixelsPerMicron);
        }
      }
      return;
    }

    if ("label" == propertyName) {
      setScaling();
      LabelToken[][] tokens = null;
      int nbs = checkStringLength(bsSelected.length());
      if (defaultColix != C.INHERIT_ALL || defaultPaletteID != 0)
        checkColixLength(defaultColix, bsSelected.length());
      if (defaultBgcolix != C.INHERIT_ALL)
        checkBgColixLength(defaultBgcolix, bsSelected.length());
      if (value instanceof Lst) {
        Lst<SV> list = (Lst<SV>) value;
        int n = list.size();
        tokens = new LabelToken[][] { null };
        for (int pt = 0, i = bsSelected.nextSetBit(0); i >= 0 && i < nbs; i = bsSelected
            .nextSetBit(i + 1)) {
          if (pt >= n) {
            setLabel(nullToken, "", i, true);
            continue;
          }
          tokens[0] = null;
          setLabel(tokens, SV.sValue(list.get(pt++)), i, true);
        }
      } else {
        String strLabel = (String) value;
        tokens = (strLabel == null || strLabel.length() == 0 ? nullToken
            : new LabelToken[][] { null });
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setLabel(tokens, strLabel, i, true);
      }
      return;
    }

    if (propertyName.startsWith("label:")) {
      // from @1.label = "xxx"
      setScaling();
      // in principle, we could make this more efficient,
      // it would be at the cost of general atom property setting
      checkStringLength(ac);
      setLabel(new LabelToken[][] { null }, propertyName.substring(6),
          ((Integer) value).intValue(), false);
      return;
    }

    if ("clearBoxes" == propertyName) {
      labelBoxes = null;
      return;
    }

    if ("translucency" == propertyName || "bgtranslucency" == propertyName) {
      // no translucency
      return;
    }

    if ("bgcolor" == propertyName) {
      isActive = true;
      if (bsBgColixSet == null)
        bsBgColixSet = BS.newN(ac);
      short bgcolix = C.getColixO(value);
      if (!setDefaults) {
        int n = checkBgColixLength(bgcolix, bsSelected.length());
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < n; i = bsSelected
            .nextSetBit(i + 1))
          setBgcolix(i, bgcolix);
      }
      if (setDefaults || !defaultsOnlyForNone)
        defaultBgcolix = bgcolix;
      return;
    }

    // the rest require bsFontSet setting

    if (bsFontSet == null)
      bsFontSet = BS.newN(ac);

    if ("fontsize" == propertyName) {
      int fontsize = ((Integer) value).intValue();
      if (fontsize < 0) {
        fids = null;
        return;
      }
      byte fid = vwr.gdata.getFontFid(fontsize);
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setFont(i, fid);
      if (setDefaults || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("font" == propertyName) {
      byte fid = ((Font) value).fid;
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setFont(i, fid);
      if (setDefaults || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("offset" == propertyName) {
      if (!(value instanceof Integer)) {
        if (!setDefaults) {
          for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
              .nextSetBit(i + 1))
            setPymolOffset(i, (float[]) value);
        }
        return;
      }

      int offset = ((Integer) value).intValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setOffsets(i, offset);
      if (setDefaults || !defaultsOnlyForNone)
        defaultOffset = offset;
      return;
    }

    if ("align" == propertyName) {
      // note that if the label is not offset, this centers the label with offset 0 0
      String type = (String) value;
      int hAlignment = (type.equalsIgnoreCase("right") ? JC.TEXT_ALIGN_RIGHT
          : type.equalsIgnoreCase("center") ? JC.TEXT_ALIGN_CENTER
              : JC.TEXT_ALIGN_LEFT);
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1))
        setHorizAlignment(i, hAlignment);
      if (setDefaults || !defaultsOnlyForNone)
        defaultAlignment = hAlignment;
      return;
    }

    if ("pointer" == propertyName) {
      int pointer = ((Integer) value).intValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setPointer(i, pointer);
      if (setDefaults || !defaultsOnlyForNone)
        defaultPointer = pointer;
      return;
    }

    if ("front" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_FRONT, TF);
      if (setDefaults || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_FRONT : 0);
      return;
    }

    if ("group" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_GROUP, TF);
      if (setDefaults || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_GROUP : 0);
      return;
    }

    if ("display" == propertyName || "toggleLabel" == propertyName) {
      // toggle
      int mode = ("toggleLabel" == propertyName ? 0 : ((Boolean) value)
          .booleanValue() ? 1 : -1);
      if (mads == null)
        mads = new short[ac];
      String strLabelPDB = null;
      LabelToken[] tokensPDB = null;
      String strLabelUNK = null;
      LabelToken[] tokensUNK = null;
      String strLabel;
      LabelToken[] tokens;
      int nstr = checkStringLength(bsSelected.length());
      short bgcolix = defaultBgcolix;
      int nbg = checkBgColixLength(bgcolix, bsSelected.length());
      short thisMad = (short) (mode >= 0 ? 1 : -1);
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (i < nstr && strings[i] != null) {
          // an old string -- toggle
          mads[i] = (short) (mode == 1 || mode == 0 && mads[i] < 0 ? 1 : -1);
        } else {
          // a new string -- turn on
          mads[i] = thisMad;
          if (atom.getGroup3(false).equals("UNK")) {
            if (strLabelUNK == null) {
              strLabelUNK = vwr.getStandardLabelFormat(1);
              tokensUNK = LabelToken.compile(vwr, strLabelUNK, '\0', null);
            }
            strLabel = strLabelUNK;
            tokens = tokensUNK;
          } else {
            if (strLabelPDB == null) {
              strLabelPDB = vwr.getStandardLabelFormat(2);
              tokensPDB = LabelToken.compile(vwr, strLabelPDB, '\0', null);
            }
            strLabel = strLabelPDB;
            tokens = tokensPDB;
          }
          strings[i] = LabelToken.formatLabelAtomArray(vwr, atom, tokens, '\0',
              null, ptTemp);
          formats[i] = strLabel;
          bsSizeSet.set(i);
          if (i < nbg && !bsBgColixSet.get(i))
            setBgcolix(i, defaultBgcolix);
        }
        atom.setShapeVisibility(vf, strings != null && i < strings.length
            && strings[i] != null && mads[i] >= 0);
      }
      return;
    }

    if ("pymolLabels" == propertyName) {
      setPymolLabels((Map<Integer, Text>) value, bsSelected);
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      labelBoxes = null;
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      fids = (byte[]) AU.deleteElements(fids, firstAtomDeleted, nAtomsDeleted);
      bgcolixes = (short[]) AU.deleteElements(bgcolixes, firstAtomDeleted,
          nAtomsDeleted);
      offsets = (int[]) AU.deleteElements(offsets, firstAtomDeleted,
          nAtomsDeleted);
      formats = (String[]) AU.deleteElements(formats, firstAtomDeleted,
          nAtomsDeleted);
      strings = (String[]) AU.deleteElements(strings, firstAtomDeleted,
          nAtomsDeleted);
      BSUtil.deleteBits(bsFontSet, bsSelected);
      BSUtil.deleteBits(bsBgColixSet, bsSelected);
      // pass to super
    }

    setPropAS(propertyName, value, bsSelected);

  }

  private int checkStringLength(int n) {
    n = Math.min(ac, n);
    if (strings == null || n > strings.length) {
      formats = AU.ensureLengthS(formats, n);
      strings = AU.ensureLengthS(strings, n);
      if (bsSizeSet == null)
        bsSizeSet = BS.newN(n);
    }
    return n;
  }

  private int checkBgColixLength(short colix, int n) {
    n = Math.min(ac, n);
    if (colix == C.INHERIT_ALL)
      return (bgcolixes == null ? 0 : bgcolixes.length);
    if (bgcolixes == null || n > bgcolixes.length)
      bgcolixes = AU.ensureLengthShort(bgcolixes, n);
    return n;
  }
  
  private void setPymolLabels(Map<Integer, Text> labels, BS bsSelected) {
    // from PyMOL reader
    setScaling();
    int n = checkStringLength(ac);
    checkColixLength((short)-1, n);
    for (int i = bsSelected.nextSetBit(0); i >= 0 && i < n; i = bsSelected
        .nextSetBit(i + 1))
      setPymolLabel(i, labels.get(Integer.valueOf(i)), null);
  }

  /**
   * Sets offset using PyMOL standard array;
   * only operates in cases where label is already defined
   * 
   * @param i
   * @param value
   */
  private void setPymolOffset(int i, float[] value) {
    // from PyMOL reader or from set labeloffset [...]
    Text text = getLabel(i);
    if (text == null) {
      if (strings == null || i >= strings.length || strings[i] == null)
        return;
      byte fid = (bsFontSet != null && bsFontSet.get(i) ? fids[i] : -1);
      if (fid < 0)
        setFont(i, fid = defaultFontId);
      text = Text.newLabel(vwr, Font.getFont3D(fid), strings[i],
          getColix2(i, atoms[i], false), getColix2(i, atoms[i], true), 0,
          scalePixelsPerMicron);
      setPymolLabel(i, text, formats[i]);
    }
    text.pymolOffset = value;
  }

  private final static LabelToken[][] nullToken = new LabelToken[][] { null };
  private boolean isScaled;
  private float scalePixelsPerMicron;
  private P3 ptTemp = new P3();
  
  private void setScaling() {
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = BS.newN(ac);
    isScaled = vwr.getBoolean(T.fontscaling);
    scalePixelsPerMicron = (isScaled ? vwr
        .getScalePixelsPerAngstrom(false) * 10000f : 0);
  }
  
  private void setPymolLabel(int i, Text t, String format) {
    if (t == null)
      return;
    String label = t.text;
    Atom atom = atoms[i];
    addString(atom, i, label, format == null ? PT.rep(label, "%", "%%") : format);
    atom.setShapeVisibility(vf, true);
    if (t.colix >= 0)
      setLabelColix(i, t.colix, PAL.UNKNOWN.id);
    setFont(i, t.font.fid);
    putLabel(i, t);
  }

  private void setLabel(LabelToken[][] temp, String strLabel, int i, boolean doAll) {
    // checkStringLength must be first
    Atom atom = atoms[i];
    LabelToken[] tokens = temp[0];
    if (tokens == null)
      tokens = temp[0] = LabelToken.compile(vwr, strLabel, '\0', null);
    String label = (tokens == null ? null : LabelToken.formatLabelAtomArray(
        vwr, atom, tokens, '\0', null, ptTemp ));
    boolean isNew = addString(atom, i, label, strLabel); 
    doAll |= isNew || label == null;
    Text text = getLabel(i);
    if (isScaled && doAll) {
      text = Text.newLabel(vwr, null, label, C.INHERIT_ALL, (short) 0, 0, scalePixelsPerMicron);
      putLabel(i, text);
    } else if (text != null && label != null) {
      text.setText(label);
      text.textUnformatted = strLabel;
    }
    if (!doAll)
      return;
    if (defaultOffset != JC.LABEL_DEFAULT_OFFSET)
      setOffsets(i, defaultOffset);
    if (defaultAlignment != JC.TEXT_ALIGN_LEFT)
      setHorizAlignment(i, defaultAlignment);
    if ((defaultZPos & JC.LABEL_ZPOS_FRONT) != 0)
      setZPos(i, JC.LABEL_ZPOS_FRONT, true);
    else if ((defaultZPos & JC.LABEL_ZPOS_GROUP) != 0)
      setZPos(i, JC.LABEL_ZPOS_GROUP, true);
    if (defaultPointer != JC.LABEL_POINTER_NONE)
      setPointer(i, defaultPointer);
    if (defaultColix != C.INHERIT_ALL || defaultPaletteID != 0)
      setLabelColix(i, defaultColix, defaultPaletteID);
    if (defaultBgcolix != C.INHERIT_ALL)
      setBgcolix(i, defaultBgcolix);
    if (defaultFontId != zeroFontId)
      setFont(i, defaultFontId);
  }

  private boolean addString(Atom atom, int i, String label, String strLabel) {
    atom.setShapeVisibility(vf, label != null);
    boolean notNull = (strLabel != null);
    boolean isNew = (strings[i] == null);
    strings[i] = label;
    // formats are put into state, but only if we are not pulling from DATA
    formats[i] = (notNull && strLabel.indexOf("%{") >= 0 ? label : strLabel);
    bsSizeSet.setBitTo(i, notNull);
    return isNew;
  }

  @Override
  public Object getProperty(String property, int index) {
    if (property.equals("offsets"))
      return offsets;
    if (property.equals("label"))
      return (strings != null && index < strings.length && strings[index] != null 
          ? strings[index] : "");
    return null;
  }

  public void putLabel(int i, Text text) {
    if (text == null)
      atomLabels.remove(Integer.valueOf(i));
    else {
      atomLabels.put(Integer.valueOf(i), text);
      text.textUnformatted = formats[i]; 
    }
  }

  public Text getLabel(int i) {
    return atomLabels.get(Integer.valueOf(i));
  }

  public void putBox(int i, float[] boxXY) {
    if (labelBoxes == null)
      labelBoxes = new Hashtable<Integer, float[]>(); 
    labelBoxes.put(Integer.valueOf(i), boxXY);
  }

  public float[] getBox(int i) {
    if (labelBoxes == null)
      return null;
    return labelBoxes.get(Integer.valueOf(i));
  }
  
  private void setLabelColix(int i, short colix, byte pid) {
    setColixAndPalette(colix, pid, i);
    // text is only created by labelsRenderer
    Text text;
    if (colixes != null && ((text = getLabel(i)) != null))
      text.colix = colixes[i];
  }

  private void setBgcolix(int i, short bgcolix) {
    bgcolixes[i] = bgcolix;
    bsBgColixSet.setBitTo(i, bgcolix != 0);
    Text text = getLabel(i);
    if (text != null)
      text.bgcolix = bgcolix;
  }

  private void setOffsets(int i, int offset) {
    
    if (offsets == null || i >= offsets.length) {
      if (offset == JC.LABEL_DEFAULT_OFFSET)
        return;
      offsets = AU.ensureLengthI(offsets, ac);
    }
    offsets[i] = (offsets[i] & JC.LABEL_FLAGS) | offset;

    Text text = getLabel(i);
    if (text != null)
      text.setOffset(offset);
  }

  private void setHorizAlignment(int i, int hAlign) {
    if (offsets == null || i >= offsets.length) {
      switch (hAlign) {
      case JC.TEXT_ALIGN_NONE:
      case JC.TEXT_ALIGN_LEFT:
        return;
      }
      offsets = AU.ensureLengthI(offsets, ac);
    }
    if (hAlign == JC.TEXT_ALIGN_NONE)
      hAlign = JC.TEXT_ALIGN_LEFT;
    offsets[i] = JC.setHorizAlignment(offsets[i], hAlign);
    Text text = getLabel(i);
    if (text != null)
      text.setAlignment(hAlign);
  }

  private void setPointer(int i, int pointer) {
    if (offsets == null || i >= offsets.length) {
      if (pointer == JC.LABEL_POINTER_NONE)
        return;
      offsets = AU.ensureLengthI(offsets, ac);
    }
    offsets[i] = JC.setPointer(offsets[i], pointer);
    Text text = getLabel(i);
    if (text != null)
      text.pointer = pointer;
  }

  private void setZPos(int i, int flag, boolean TF) {
    if (offsets == null || i >= offsets.length) {
      if (!TF)
        return;
      offsets = AU.ensureLengthI(offsets, ac);
    }
    offsets[i] = JC.setZPosition(offsets[i], TF ? flag : 0);
  }

  private void setFont(int i, byte fid) {
    if (fids == null || i >= fids.length) {
      if (fid == zeroFontId)
        return;
      fids = AU.ensureLengthByte(fids, ac);
    }
    fids[i] = fid;
    bsFontSet.set(i);
    Text text = getLabel(i);
    if (text != null) {
      text.setFontFromFid(fid);
    }
  }

  @Override
  public void setAtomClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0;) {
      String label = strings[i];
      if (label != null && ms.at.length > i
          && !ms.isAtomHidden(i))
        ms.at[i].setClickable(vf);
    }
  }

//  @Override
//  public String getShapeState() {
//    // not implemented -- see org.jmol.viewer.StateCreator
//    return null;
//  }

  private int pickedAtom = -1;
  private int pickedOffset = 0;
  private int pickedX;
  private int pickedY;
  
  @Override
  public synchronized boolean checkObjectDragged(int prevX, int prevY, int x,
                                                 int y, int dragAction,
                                                 BS bsVisible) {

    if (vwr.getPickingMode() != ActionManager.PICKING_LABEL
        || labelBoxes == null)
      return false;
    // mouse down ?
    if (prevX == Integer.MIN_VALUE) {
      int iAtom = findNearestLabel(x, y);
      if (iAtom >= 0) {
        pickedAtom = iAtom;
        vwr.acm.setDragAtomIndex(iAtom);
        pickedX = x;
        pickedY = y;
        pickedOffset = (offsets == null || pickedAtom >= offsets.length ? 
            JC.LABEL_DEFAULT_OFFSET : offsets[pickedAtom]);
        return true;
      }
      return false;
    }
    // mouse up ?
    if (prevX == Integer.MAX_VALUE) {
      pickedAtom = -1;
      return false;
    }
    if (pickedAtom < 0)
      return false;
    move2D(pickedAtom, x, y);
    return true;
  }
                         
  private int findNearestLabel(int x, int y) {
    if (labelBoxes == null)
      return -1;
    float dmin = Float.MAX_VALUE;
    int imin = -1;
    float zmin = Float.MAX_VALUE;
    float afactor = (vwr.antialiased ? 2 : 1);
    for (Map.Entry<Integer, float[]> entry : labelBoxes.entrySet()) {
      if (!atoms[entry.getKey().intValue()].isVisible(vf | Atom.ATOM_INFRAME_NOTHIDDEN))
        continue;
      float[] boxXY = entry.getValue();
      float dx = (x - boxXY[0])*afactor;
      float dy = (y - boxXY[1])*afactor;
      if (dx <= 0 || dy <= 0 || dx >= boxXY[2] || dy >= boxXY[3] || boxXY[4] > zmin)
        continue;
      zmin = boxXY[4];
      float d = Math.min(Math.abs(dx - boxXY[2]/2), Math.abs(dy - boxXY[3]/2));
      if (d <= dmin) {
        dmin = d;
        imin = entry.getKey().intValue();
      }
    }
    return imin;
  }

  private void move2D(int pickedAtom, int x, int y) {
    int xOffset = JC.getXOffset(pickedOffset);
    int yOffset = JC.getYOffset(pickedOffset);        
    xOffset += x - pickedX;
    yOffset -= y - pickedY;
    int offset = JC.getOffset(xOffset, yOffset, true);
    setOffsets(pickedAtom, offset);
  }

  public short getColix2(int i, Atom atom, boolean isBg) {
    short colix;
    if (isBg) {
      colix = (bgcolixes == null || i >= bgcolixes.length) ? 0 : bgcolixes[i];
    } else {
      colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      colix = C.getColixInherited(colix, atom.colixAtom);
      if (C.isColixTranslucent(colix))
        colix = C.getColixTranslucent3(colix, false, 0);
    }
    return colix;
  }
  
}
