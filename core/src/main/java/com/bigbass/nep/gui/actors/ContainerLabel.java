package com.bigbass.nep.gui.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bigbass.nep.gui.ColorCache;
import com.bigbass.nep.gui.listeners.HoverListener;

public class ContainerLabel extends CustomContainer<CustomLabel> {

	private HoverListener hoverListener;
	private Drawable hover;
	
	public CustomLabel label;
	
	public ContainerLabel(Skin skin){
		this(skin, false);
	}
	
	public ContainerLabel(Skin skin, boolean isHoverable){
		super(skin);
		
		label = new CustomLabel(skin);
		setActor(label);
		
		pad(1, 3, 1, 3);
		
		label.setWrap(true);
		
		if(isHoverable){
			hover = skin.newDrawable("whiteBackground", 1, 1, 1, 0.5f);
			hoverListener = new HoverListener();
			this.addListener(hoverListener);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha){
		super.draw(batch, parentAlpha);
		if(hoverListener != null && hoverListener.isOver()){
			hover.draw(batch, getX(), getY(), getWidth(), getHeight());
		}
	}
	
	@Override
	public Container<CustomLabel> minWidth(float minWidth){
		return super.minWidth(minWidth - (this.getPadLeft() + this.getPadRight()));
	}
	
	public void setForegroundColor(Color color){
		this.label.setForegroundColor(ColorCache.getForegroundColor(color));
	}
}