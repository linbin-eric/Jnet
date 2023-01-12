package com.jfirer.jnet;

import com.jfirer.jnet.common.recycler.FastThreadLocalTest;
import com.jfirer.jnet.common.recycler.RecycleTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({BaseTest.class, BufferSuiteTest.class, RecycleTest.class, FastThreadLocalTest.class})
public class AllTest
{}
