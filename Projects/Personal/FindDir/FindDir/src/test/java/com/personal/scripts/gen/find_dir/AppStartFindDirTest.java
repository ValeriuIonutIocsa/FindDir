package com.personal.scripts.gen.find_dir;

import org.junit.jupiter.api.Test;

class AppStartFindDirTest {

	@Test
	void testMain() {

		final String[] args;
		final int input = Integer.parseInt("1");
		if (input == 1) {

			args = new String[] {
                    "C:\\IVI\\",
                    ".*\\\\io-utils\\\\io-utils"
            };

		} else {
			throw new RuntimeException();
		}

		AppStartFindDir.main(args);
	}
}
