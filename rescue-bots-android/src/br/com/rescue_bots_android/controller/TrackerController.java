package br.com.rescue_bots_android.controller;

import java.util.List;

import android.app.Activity;
import android.util.Log;
import br.com.rescue_bots_android.sqlite.TrackerRepositorio;
import br.com.rescuebots.pojo.Tracker;

public class TrackerController  extends Controller{

	public TrackerController(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}
	
	public long inserir(Tracker bean){
		if(bean!=null){
			TrackerRepositorio repository = null;
			try {
				repository = new TrackerRepositorio(activity);
				return repository.inserir(bean);
			} catch (Exception e) {
				Log.e("ERROR", e.getLocalizedMessage(),e);
			}finally{
				repository.close();
			}	
		}
		return 0;
	}
	public void removeAll() {
			TrackerRepositorio repository = null;
			try {
				repository = new TrackerRepositorio(activity);
				repository.removeAll();
			} catch (Exception e) {
				Log.e("ERROR", e.getLocalizedMessage(),e);
			}finally{
				repository.close();
			}	
	}
	public void selectAll(List<Tracker> result) {
		TrackerRepositorio repository = null;
		try {
			repository = new TrackerRepositorio(activity);
			repository.selectAll(result);
		} catch (Exception e) {
			Log.e("ERROR", e.getLocalizedMessage(),e);
		}finally{
			repository.close();
		}	
	}
}
