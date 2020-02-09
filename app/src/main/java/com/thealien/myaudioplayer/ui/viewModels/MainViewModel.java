package com.thealien.myaudioplayer.ui.viewModels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {


    MutableLiveData<String> mediaTitle;
    MutableLiveData<String> mediaDescription;
    MutableLiveData<Long> seekBarProgress;
    MutableLiveData<Long> seekBarMaxDuration;


}
