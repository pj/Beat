package beat.collector;

public enum EventType {
	methodCall,
	methodCallExit,
	methodEntered,
	methodExit,
	lockWaitStart,
	lockWaitEnd,
	
	lockNotify,
	lockNotifyAll,
	
	threadSleepStart,
	threadSleepEnd,
	
	threadJoinStart,
	threadJoinEnd,
	
	threadStart,
	
	threadStartCall,
	threadStartEntered,
	threadRunEntered,
	threadRunExit,
	
//	threadRunEnter,
//	threadRunEnd,

	programEntered,
	programExit,
	
	forLoopIn,
	forLoopOut,
	whileLoopIn,
	whileLoopOut,
	doLoopIn,
	doLoopOut,
	
	loopIn,
	loopOut,
	
//	forLoopStart,
//	forLoopEnd,
//	whileLoopStart,
//	whileLoopEnd,
//	doLoopStart,
//	doLoopEnd,
	
	ifStatement,
	elseStatement,
	caseStatement,
	
//	contextSwitch,
	contextSwitchIn,
	contextSwitchOut,
	
	synchronizedBlockAcquire,
	synchronizedBlockEntered,
	synchronizedBlockExit,
	
	synchronizedMethodCalled,
	synchronizedMethodEntered,
	synchronizedMethodExit,
	
	synchronizedMethodWaitStart,
	synchronizedMethodWaitEnd,
	
	fieldWritten,
	fieldRead,
	
	returnStatement,
	
	constructorEntered,
	constructorExit,
	exceptionEntered,
	threadDeathException,
	thrown
}
