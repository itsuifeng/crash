/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.cmdline.matcher;

import org.crsh.cmdline.ClassDescriptor;
import org.crsh.cmdline.EmptyCompleter;
import org.crsh.cmdline.binding.ClassFieldBinding;
import org.crsh.cmdline.binding.MethodArgumentBinding;
import org.crsh.cmdline.MethodDescriptor;
import org.crsh.cmdline.spi.Completer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Matcher<T> {

  /** . */
  private final MatcherFactory<T, ClassFieldBinding> analyzer;

  /** . */
  private final ClassDescriptor<T> descriptor;

  /** . */
  private final String mainName;

  public Matcher(ClassDescriptor<T> descriptor) {
    this(null, descriptor);
  }

  public Matcher(String mainName, ClassDescriptor<T> descriptor) {
    this.analyzer = new MatcherFactory<T, ClassFieldBinding>(descriptor);
    this.descriptor = descriptor;
    this.mainName = mainName;
  }

  public Map<String, String> complete(String s) throws CmdCompletionException {
    return complete(EmptyCompleter.getInstance(), s);
  }

  public Map<String, String> complete(Completer completer, String s) throws CmdCompletionException {

    //
    StringCursor cursor = new StringCursor(s);

    // Read all common options we are able to
    Map<String, String> completions = analyzer.completeOptions(completer, cursor);

    //
    if (completions != null) {
      return completions;
    }

    //
    MethodDescriptor<T> main = mainName != null ? descriptor.getMethod(mainName) : null;

    //
    MethodDescriptor<T> method = null;
    Pattern p = Pattern.compile("^\\s*(\\S+|$)");
    java.util.regex.Matcher m = p.matcher(cursor);
    if (m.find()) {
      String name = cursor.group(m, 1);
      method = descriptor.getMethod(name);
      if (method == null || method == main) {
        Map<String, String> a = new HashMap<String, String>();
        for (MethodDescriptor<T> candidate : descriptor.getMethods()) {
          if (candidate != main && candidate.getName().startsWith(name)) {
            a.put(candidate.getName().substring(name.length()), " ");
          }
        }
        if (a.size() > 0) {
          return a;
        }
      } else {
        cursor.skip(m.end(1));
        if (cursor.isEmpty()) {
          return Collections.singletonMap("", " ");
        }
      }
    }

    //
    if (method == null) {
      method = main;
    }

    //
    if (method != null) {

      MatcherFactory<T, MethodArgumentBinding> methodAnalyzer = new MatcherFactory<T, MethodArgumentBinding>(method);
      completions = methodAnalyzer.completeOptions(completer, cursor);

      //
      if (completions == null) {
        completions = methodAnalyzer.completeArguments(completer, cursor);
      }
    }

    //
    if (completions == null) {
      completions = Collections.emptyMap();
    }

    //
    return completions;
  }

  public CommandMatch<T, ?, ?> match(String s) {

    //
    StringCursor cursor = new StringCursor(s);

    // Read all common options we are able to
    List<OptionMatch<ClassFieldBinding>> options = analyzer.analyzeOptions(cursor);

    List<OptionMatch<MethodArgumentBinding>> methodOptions = null;
    List<ArgumentMatch<MethodArgumentBinding>> methodArguments = null;
    MethodDescriptor<T> method = null;
    Pattern p = Pattern.compile("^\\s*(\\S+)");
    java.util.regex.Matcher m = p.matcher(cursor);
    if (m.find()) {
      String f = cursor.group(m, 1);
      method = descriptor.getMethod(f);
      if (method != null) {
        cursor.skip(m.end(1));
      }
    }

    // Try to consume with main method name then
    boolean implicit;
    if (method == null) {
      method = descriptor.getMethod(mainName);
      implicit = true;
    } else {
      implicit = false;
    }

    //
    if (method != null) {
      ClassMatch<T> owner = new ClassMatch<T>(descriptor, options, Collections.<ArgumentMatch<ClassFieldBinding>>emptyList(), cursor.getOriginal());
      MatcherFactory<T, MethodArgumentBinding> methodAnalyzer = new MatcherFactory<T, MethodArgumentBinding>(method);
      methodOptions = methodAnalyzer.analyzeOptions(cursor);
      methodArguments = methodAnalyzer.analyzeArguments(cursor);
      return new MethodMatch<T>(owner, method, implicit, methodOptions, methodArguments, cursor.getOriginal());
    } else {
      List<ArgumentMatch<ClassFieldBinding>> arguments = analyzer.analyzeArguments(cursor);
      return new ClassMatch<T>(descriptor, options, arguments, cursor.getOriginal());
    }
  }

}
