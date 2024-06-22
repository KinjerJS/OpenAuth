/*
 * Copyright 2015-2021 Adrien 'Litarvan' Navratil
 *
 * This file is part of OpenAuth.

 * OpenAuth is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAuth.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.litarvan.openauth.microsoft;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;

/*
 * Had to use Swing here, JavaFX is meant to have an 'Application' but only one can exist.
 * Creating one would break compatibility with JavaFX apps (which already have their own
 * class), and letting the user do so would break compatibility with Swing apps.
 *
 * This method makes the frame compatible with pretty much everything.
 */

public class LoginFrame extends JFrame
{
    private CompletableFuture<String> future;
    private boolean completed;

    private final CefApp cefApp;

    public LoginFrame(CefApp cefApp) {
        this.cefApp = cefApp;
        this.setTitle("Microsoft Authentication");
        this.setSize(750, 750);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public CompletableFuture<String> start(String url)
    {
        if (this.future != null) {
            return this.future;
        }

        this.future = new CompletableFuture<>();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(!completed)
                    future.complete(null);
            }
        });

        SwingUtilities.invokeLater(() -> init(url));
        return this.future;
    }

    protected void init(String url) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        CefClient client = this.cefApp.createClient();
        System.out.println(url);

        CefBrowser browser = client.createBrowser(url, false, false);
        Component browserUI = browser.getUIComponent();
        browserUI.setBounds(0, 0, this.getWidth(), this.getHeight());
        layeredPane.add(browser.getUIComponent(), JLayeredPane.DEFAULT_LAYER);
        this.setContentPane(layeredPane);
        CefMessageRouter msgRouter = CefMessageRouter.create();

        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                String url = browser.getURL();
                if (url.contains("access_token")) {
                    future.complete(url);
                    completed = true;
                    SwingUtilities.invokeLater(() -> dispose());
                }
            }
        });
        client.addMessageRouter(msgRouter);

        this.setVisible(true);
        this.repaint();
        this.revalidate();
    }
}
