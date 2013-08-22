package com.meneguello.coi;

import org.jooq.impl.Executor;

public abstract class FallibleTransaction<T> extends Transaction<T> {
	
	public FallibleTransaction() {
		super();
	}

	public FallibleTransaction(boolean atomic) {
		super(atomic);
	}

	@Override
	protected T execute(Executor database) {
		try {
			return executeFallible(database);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected abstract T executeFallible(Executor database) throws Exception;

}
