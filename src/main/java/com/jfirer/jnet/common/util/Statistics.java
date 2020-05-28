package com.jfirer.jnet.common.util;

public class Statistics
{
    private int[] totals;
    private int[] durations;

    public Statistics(int... durations)
    {
        this.durations = durations;
        totals = new int[durations.length];
    }

    public void count(int num)
    {
        int length = durations.length;
        for (int i = 0; i < length; i++)
        {
            if (durations[i] >= num)
            {
                totals[i] += 1;
                break;
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < durations.length; i++)
        {
            stringBuilder.append("<").append(durations[i]).append(":").append(totals[i]).append(",");
        }
        return stringBuilder.toString();
    }
}
