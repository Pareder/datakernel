package io.datakernel.di.impl;

@FunctionalInterface
public interface BindingCompiler<R> {

	CompiledBinding<R> compile(CompiledBindingLocator compiledBindings, boolean threadsafe, int scope, int index);
}
