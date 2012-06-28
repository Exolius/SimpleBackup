package com.exolius.simplebackup;

import java.util.ArrayList;
import java.util.List;

/**
 * This a thread pool mad by gravypod.
 * You can add runnable processes into it and they will run from the list.
 * This is also used im my PAINIS project, a Bloat-Hello-World project.
 * @author <a href='www.github.com/gravypod/'>gravypod</a>
 *
 */
public class ThreadPool extends Thread {

	private static volatile List<Runnable> taskList = new ArrayList<Runnable>();

	protected synchronized static void addTask(Runnable newTask) {
		ThreadPool.taskList.add(newTask);
	}

	ThreadPool() {
		this.start();
	}

	@Override
	public void run() {
		do {
			while (ThreadPool.taskList.size() > 0) {
				ThreadPool.taskList.get(0).run();
				ThreadPool.taskList.remove(0);
			}
		} while (true);
	}
}
