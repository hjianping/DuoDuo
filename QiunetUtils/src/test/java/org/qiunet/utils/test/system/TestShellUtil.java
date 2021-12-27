package org.qiunet.utils.test.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.qiunet.utils.system.ShellUtil;
import org.qiunet.utils.test.base.BaseTest;

/**
 * @author qiunet
 *         Created on 16/11/6 13:10.
 */
public class TestShellUtil extends BaseTest{

	@Test
	@EnabledOnOs({OS.LINUX, OS.MAC})
	public void testExecShell(){
		String[] cmd = {"ls"};
		String ret = ShellUtil.execShell(cmd);
		logger.info(ret);
		Assertions.assertNotNull(ret);
	}
}
