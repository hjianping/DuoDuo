package org.qiunet.cfg.test.xd;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qiunet.utils.scanner.ClassScanner;
import org.qiunet.utils.scanner.ScannerType;

/***
 *
 *
 * @author qiunet
 * 2020-04-23 18:26
 ***/
public class TestXdManager {

	@BeforeAll
	public static void preExec() throws Throwable {
		ClassScanner.getInstance(ScannerType.CFG).scanner();
	}
	@Test
	public void test(){
		TestXdService.instance.testReward();
		TestXdService.instance.testSimpleMap();
		TestXdService.instance.testNestMap();
		TestXdService.instance.testNestList();
	}
}
