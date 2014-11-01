import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.MemoryImageSource;
import javax.swing.JFrame;
import java.util.ArrayList;

/** A game of Ducks. The mouse is used to aim and shoot all the ducks before time runs out.
  * There is a limited number of shots available per round.
  */

public class Ducks extends Applet implements MouseMotionListener, MouseListener, KeyListener, Runnable {

	private Thread gameThread;
	private Image dbImage;
	private Graphics dbGraphics;
	private Image backgroundImage, cursorImage;
	private boolean newGame, nextLevel, lose, timeLose;
	private int numDucks;
	private int xMouse, yMouse;
	private int shotsLeft, score, level, time;
	private ArrayList<Duck> ducks;

	/* Applet initialization. Provide the size of the applet, add Event Listeners,
	 * set a blank cursor, and initialize game variables.
	 */
	public void init() {
		setSize(1192, 728); //change this for adaptation?
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		cursorImage = Toolkit.getDefaultToolkit().createImage("target.PNG").getScaledInstance(60, 60, Image.SCALE_SMOOTH);
		int[] pixels = new int[16*16];
		Image blank = createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank cursor");
		setCursor(blankCursor);

		backgroundImage = Toolkit.getDefaultToolkit().createImage("duckBackground.JPG");
		newGame = true;
		nextLevel = false;
		lose = false;
		timeLose = false;
		numDucks = 1;
		score = 0;
		level = 1;
		time = 300;
		ducks = new ArrayList<Duck>();
	}

	/* Applet main loop. Paint images and strings. Prompt user for input if necessary.
	 */
	public void paint(Graphics g) {
		g.drawImage(backgroundImage, 0, 0, this);
		g.drawImage(cursorImage, xMouse, yMouse, this);
		for (Duck duck : ducks) {
			g.drawImage(duck.getCurrentImage(), duck.x(), duck.y(), this);
		}
		if (newGame) {
			g.setFont(new Font("Ravie", Font.PLAIN, 25));
			g.drawString("Press SPACE to start...", 400, 400);
			numDucks = 1;
			score = 0;
			level = 1;
		} else if (nextLevel) {
			if(level < 10) {
				g.drawString("Next level. Press SPACE to continue...",200,400);
			} else {
				g.drawString("INSANITY LEVEL REACHED! Press SPACE to continue...",200,400);
			}
		} else if(lose) {
			if(timeLose) {
				g.drawString("DUCK! It's Game Over: You ran out of time.",300,400);
			} else {
				g.drawString("DUCK! It's Game Over: You ran out of bullets.",300,400);
			}
			g.drawString("Press SPACE to play again...",400,500);
		}

		g.drawString("Shots Left: " + shotsLeft, 50, 680);
		g.drawString("Score: " + score, 530, 680);
		g.drawString("Level: " + level, 950, 680);
		g.drawString("Time: ", 30, 350);
		g.drawString((time / 100) + "", 60, 390);
	}

	/* Double-buffer the Applet
	 */
	public void update(Graphics g) {
		if (dbImage == null) {
			dbImage = createImage(getSize().width, getSize().height);
			dbGraphics = dbImage.getGraphics();
		}
		dbGraphics.setColor(getBackground());
		dbGraphics.fillRect(0, 0, getSize().width, getSize().height);
		dbGraphics.setColor(getForeground());
		paint(dbGraphics);
		g.drawImage(dbImage, 0, 0, this);
	}

	/* Update mouse coordinates.
	 */
	public void mouseMoved(MouseEvent e) {
		xMouse = e.getX();
		yMouse = e.getY();
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseDragged(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

	/* Respond to a shot being fired, possibly ending the game.
	 */
	public void mousePressed(MouseEvent e) {
		if (!newGame && !nextLevel && !lose) { //a shot is fired
			int x = e.getX() + 29; //center of target
			int y = e.getY() + 29; //center of target
			for (int i = 0; i < ducks.size(); i++) {
				int duckx = ducks.get(i).x();
				int ducky = ducks.get(i).y();
				if(x > duckx && x < duckx + 90 && y > ducky && y < ducky + 110) { //a shot hit a duck
					if(ducks.size() == 1) {
						shotsLeft++;
					}
					ducks.get(i).kill();
					score++;
				}
			}
			shotsLeft--;
		}
		if (shotsLeft == 0 && !nextLevel && !ducks.isEmpty()) { //no more shots available
			lose = true;
			timeLose = false;
			ducks.clear();
		}
	}

	/* Respond to the spacebar being pressed, start a new game or new level.
	 */
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode == 32) { //spacebar
			if (lose) { //start a new game
				newGame = true;
			}
			if (level < 10) {
				if (level % 2 == 0) {
					numDucks++;
				}
				shotsLeft = 2*numDucks;
			}
			else {
				numDucks += 20;
				shotsLeft = (int)(1.25 * numDucks);
			}
			if (newGame || nextLevel) {
				ducks.clear();
				if (newGame) {
					level = 1;
					numDucks = 1;
					score = 0;
				}
				for (int i = 0; i < numDucks; i++) {
					ducks.add(new Duck(this, "Duck-" + i));
					ducks.get(i).start();
				}
				time = 200 * numDucks + 100;
				newGame = false;
				nextLevel = false;
				lose = false;
			}
		}
	}

	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	/* Start the Applet thread.
	 */
	public void start() {
		if (gameThread == null) {
			gameThread = new Thread(this);
			gameThread.start();
		}
	}

	/* Applet thread's main loop.
	 */
	public void run() {
		while (gameThread != null) {
			for (int i = 0; i < ducks.size(); i++) {
				if (ducks.get(i).getState().equals(Thread.State.TERMINATED)) {
					ducks.remove(i);
				}
			}
			if (ducks.isEmpty() && !newGame && !nextLevel && !lose) {
				nextLevel = true;
				level++;
			}
			if (time == 0) {
				lose = true;
				timeLose = true;
			}
			try {
				Thread.sleep(20);
				if(!ducks.isEmpty() && !newGame && !nextLevel && !lose) {
					time -= 2; //make up for lost time from sleeping
				}
			}
			catch(InterruptedException e) {
			}
			repaint();
		}
	}

	/* Kill the Applet thread.
	 */
	public void stop() {
		gameThread = null;
	}

	/* A Duck that flies around in the Applet and dies if it is shot.
	 */
    public class Duck extends Thread {
		private Applet applet;
		private int appletWidth, appletHeight;
		private Image wingsUp, wingsDown, dead, currentImage; //different "states" a duck is in
		private int x, y;
		private double dx, dy;
		private int wingCounter, bangCounter;
		private boolean shot;

		/* Create a Duck with an Applet environment and a name.
		 */
		public Duck(Applet a, String name) {
			super(name);
			applet = a;
			appletWidth = a.getWidth();
			appletHeight = a.getHeight();
			wingsUp = Toolkit.getDefaultToolkit().createImage("wingsup.PNG");
			wingsDown = Toolkit.getDefaultToolkit().createImage("wingsdown.PNG");
			dead = Toolkit.getDefaultToolkit().createImage("bang.PNG");
			currentImage = wingsUp;
			int randomCorner = ((int)(4*Math.random()));
			switch (randomCorner) {
				case 0:
					x = 0;
					y = 0;
					dx = 10*Math.random() - 1;
					dy = 10*Math.random() - 1;
					break;
				case 1:
					x = appletWidth;
					y = 0;
					dx = -10*Math.random() - 1;
					dy = 10*Math.random() - 1;
					break;
				case 2:
					x = 0;
					y = appletHeight - 300;
					dx = -10*Math.random() - 1;
					dy = -10*Math.random() - 1;
					break;
				case 3:
					x = appletWidth;
					y = appletHeight - 300;
					dx = 10*Math.random() - 1;
					dy = -10*Math.random() - 1;
			}
			shot = false;
			wingCounter = 0;
			bangCounter = 0;
		}

		/* Get the x coordinate of the Duck.
		 */
		public int x() {
			return x;
		}

		/* Get the y coordinate of the Duck.
		 */
		public int y() {
			return y;
		}

		/* Get the Image of the Duck, flying or dead.
		 */
		public Image getCurrentImage() {
			return currentImage;
		}

		/* Duck's primary method.
		 */
		public void fly() {
			if (!shot) {
				if (wingCounter == 50) {
					if (currentImage == wingsUp) {
						currentImage = wingsDown;
					} else if (currentImage == wingsDown) {
						currentImage = wingsUp;
					}
					wingCounter = 0;
				}
				x += dx;
				y += dy;
				//bounce off the sides of the Applet at random angles
				if (x < 0) {
					x = 0;
					dx = 10*Math.random() + 1;
					int sign = (int)Math.pow(-1, (int)(2*Math.random()));
					dy = sign*10*Math.random() + sign;
				}
				if (x > appletWidth) {
					x = appletWidth;
					dx = -10*Math.random() - 1;
					int sign = (int)Math.pow(-1, (int)(2*Math.random()));
					dy = sign*10*Math.random() + sign;
				}
				if (y < 0) {
					y = 0;
					dy = 10*Math.random() + 1;
					int sign = (int)Math.pow(-1, (int)(2*Math.random()));
					dx = sign*10*Math.random() + sign;
				}
				if (y > appletHeight-200) {
					y = appletHeight-200;
					dy = -10*Math.random() - 1;
					int sign = (int)Math.pow(-1, (int)(2*Math.random()));
					dx = sign*10*Math.random() + sign;
				}
			} else {
				currentImage = dead;
				bangCounter++;
			}
		}

		/* Kill the duck.
		 */
		public void kill() {
			shot = true;
		}

		/* Thread's main loop.
		 */
		public void run() {
			while (bangCounter < 15) {
				fly();
				try {
					Thread.sleep(50/level); //ducks speed up as level increases
					wingCounter++;
				}
				catch(InterruptedException e) {
				}
			}
		}
	}

	/* Pack the Applet into a JFrame and run it.
	 */
	public static void main(String[] args){
		JFrame frame = new JFrame("Ducks");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		Applet thisApplet = new Ducks();
		frame.getContentPane().add(thisApplet, BorderLayout.CENTER);
		thisApplet.init();
		frame.setSize(thisApplet.getSize());
		thisApplet.start();
		frame.setVisible(true);
	}
}
