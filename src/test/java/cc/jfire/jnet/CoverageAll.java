package cc.jfire.jnet;

import cc.jfire.jnet.common.recycler.FastThreadLocalTest;
import cc.jfire.jnet.common.recycler.RecycleTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({BaseTest.class, CloseTest.class, BufferSuiteTest.class, RecycleTest.class, FastThreadLocalTest.class})
public class CoverageAll
{
}