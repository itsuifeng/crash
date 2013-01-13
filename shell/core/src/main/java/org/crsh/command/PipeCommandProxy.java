/*
 * Copyright (C) 2012 eXo Platform SAS.
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

package org.crsh.command;

import org.crsh.io.InteractionContext;
import org.crsh.io.Consumer;

import java.io.IOException;

class PipeCommandProxy<C, P> implements CommandInvoker<C, P> {

  /** . */
  private final InteractionContext<P> innerContext;

  /** . */
  private final CommandInvoker<C, P> delegate;

  /** . */
  private final Consumer<C> next;

  PipeCommandProxy(InteractionContext<P> innerContext, CommandInvoker<C, P> delegate, Consumer<C> next) {
    this.innerContext = innerContext;
    this.delegate = delegate;
    this.next = next;
  }

  void fire() {
    open(innerContext);
  }

  public void setSession(CommandContext session) {
    delegate.setSession(session);
  }

  public Class<P> getProducedType() {
    return delegate.getProducedType();
  }

  public Class<C> getConsumedType() {
    return delegate.getConsumedType();
  }

  public void setPiped(boolean piped) {
    delegate.setPiped(piped);
  }

  public void open(InteractionContext<P> context) {
    if (next != null && next instanceof PipeCommandProxy) {
      ((PipeCommandProxy)next).fire();
    }
    delegate.open(context);
  }

  public void provide(C element) throws ScriptException, IOException {
    delegate.provide(element);
  }

  public void flush() throws ScriptException, IOException {
    delegate.flush();
    if (next != null && next instanceof PipeCommand) {
      ((PipeCommand)next).flush();
    }
  }

  public void close() throws ScriptException {
    delegate.close();
    if (next != null && next instanceof PipeCommand) {
      ((PipeCommand)next).close();
    }
  }
}
