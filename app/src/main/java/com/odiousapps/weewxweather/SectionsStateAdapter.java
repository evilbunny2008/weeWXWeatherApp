package com.odiousapps.weewxweather;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

@DontObfuscate
class SectionsStateAdapter extends FragmentStateAdapter
{
	private final ArrayList<Fragment> arrayList = new ArrayList<>();

	public SectionsStateAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle)
	{
		super(fragmentManager, lifecycle);
	}

	public void addFragment(Fragment fragment)
	{
		arrayList.add(fragment);
	}

	@NonNull
	@Override
	public Fragment createFragment(int position)
	{
		return arrayList.get(position);
	}

	@Override
	public int getItemCount()
	{
		return arrayList.size();
	}
}
