package com.pmplus.datasync;

import java.util.Date;

import org.junit.Test;

public class DateTest {

	@Test
	public void test() throws Exception {
		Date date=new Date(0);
		System.out.println(date.toLocaleString());
	}
}
