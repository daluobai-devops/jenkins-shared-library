package com.daluobai.jenkinslib.lib.hutool.core.thread.lock;

import com.daluobai.jenkinslib.lib.hutool.core.thread.lock.NoLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 无锁的读写锁实现
 *
 * @author looly
 * @since 5.8.0
 */
public class NoReadWriteLock implements ReadWriteLock {
	@Override
	public Lock readLock() {
		return com.daluobai.jenkinslib.lib.hutool.core.thread.lock.NoLock.INSTANCE;
	}

	@Override
	public Lock writeLock() {
		return NoLock.INSTANCE;
	}
}
