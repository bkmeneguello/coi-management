package com.meneguello.coi;

import org.jooq.DSLContext;

public abstract class FallibleTransaction<T> extends Transaction<T> {
	
	public FallibleTransaction() {
		super();
	}

	public FallibleTransaction(boolean atomic) {
		super(atomic);
	}

	@Override
	protected T execute(DSLContext database) {
		try {
			return executeFallible(database);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected abstract T executeFallible(DSLContext database) throws Exception;

}
