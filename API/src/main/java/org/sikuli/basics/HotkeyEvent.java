/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * RaiMan 2013, 2015
 */
package org.sikuli.basics;

/**
 *
 * use org.sikuli.script.HotkeyEvent instead
 * @deprecated
 */
@Deprecated
public class HotkeyEvent {
   public int keyCode;
   public int modifiers;

   public HotkeyEvent(int code_, int mod_){
      init(code_, mod_);
   }

   void init(int code_, int mod_){
      keyCode = code_;
      modifiers = mod_;
   }
}

