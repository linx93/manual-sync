package com.tuochexia.manualsync;

import com.tuochexia.manualsync.entity.po.FinanceReconciliationPO;
import com.tuochexia.manualsync.mapper.FinanceReconciliationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ManualSyncApplicationTests {
	@Autowired
	private FinanceReconciliationMapper fiMapper;

	@Test
	void contextLoads() {
	}


	@Test
	void selectTest(){
		FinanceReconciliationPO f = fiMapper.selectOneById(1000L);
		System.out.println(f);
	}
}
