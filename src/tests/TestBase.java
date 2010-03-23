package tests;

public class TestBase 
{
	protected void test(boolean condition, String testName)
	{
		if (condition)
			System.out.println(testName + "\t" + "Passed");
		else
			System.out.println(testName + "\t" + "Failed");
	}
	
	protected void pass(String testName)
	{
		test(true, testName);
	}
	
	protected void fail(String testName)
	{
		test(false, testName);
	}
}
