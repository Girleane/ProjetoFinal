import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SwingChatClient extends UnicastRemoteObject implements IClientClient {
    private JFrame frame;
    private JLabel logo;
    private Map<String, List<String>> conversations = new HashMap<>();
    private String selectedContact = null;
    private JPanel messagesPanel;
    private JTextField inputField;
    private DefaultListModel<String> contactModel = new DefaultListModel<>();
    private JList<String> contactList = new JList<>(contactModel);
    private JButton btnStatus = new JButton("Entrar na Rede");
    private String myName;
    private IChatServer server;
    private boolean isOnline = false;

    // Cores Estilo Rede Social
    Color bgDark = new Color(24, 25, 26); // Estilo Facebook/Discord Dark
    Color bgSidebar = new Color(36, 37, 38);
    Color accentBlue = new Color(45, 136, 255);
    Color textColor = Color.WHITE;

    public SwingChatClient() throws RemoteException {
        super();
        setupUI();
    }

    private void setupUI() {
        frame = new JFrame("SocialLink Simulation");
        frame.setSize(850, 600);
        frame.getContentPane().setBackground(bgDark);
        frame.setLayout(new BorderLayout(5, 5));

        // --- SIDEBAR (CONTATOS) ---
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(bgSidebar);
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

        JLabel lblFriends = new JLabel("  AMIGOS");
        lblFriends.setForeground(Color.GRAY);
        lblFriends.setFont(new Font("Arial", Font.BOLD, 12));
        lblFriends.setPreferredSize(new Dimension(0, 40));
        
        contactList.setBackground(bgSidebar);
        contactList.setForeground(Color.WHITE);
        contactList.setSelectionBackground(accentBlue);
        contactList.setFixedCellHeight(40);
        contactList.setFont(new Font("Arial", Font.PLAIN, 14));
        contactList.setBorder(new EmptyBorder(5, 10, 5, 10));
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedContact = contactList.getSelectedValue();
                loadConversation(selectedContact);
            }
        });

        JButton btnAdd = new JButton("+ Seguir Novo Amigo");
        styleButton(btnAdd, Color.DARK_GRAY);
        btnAdd.addActionListener(e -> addContactAction());

        JButton btnRemove = new JButton("- Remover Amigo");
        styleButton(btnRemove, Color.DARK_GRAY);
        btnRemove.addActionListener(e -> {
            String sel = contactList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(frame, "Selecione um contato para remover.");
                return;
            }
            try {
                server.removerContato(myName, sel);
                refresh();
                JOptionPane.showMessageDialog(frame, "Voce deixou de seguir " + sel);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Erro ao remover contato: " + ex.getMessage());
            }
        });

        JPanel southBtns = new JPanel(new GridLayout(2,1,5,5));
        southBtns.setOpaque(false);
        southBtns.add(btnAdd);
        southBtns.add(btnRemove);

        side.add(lblFriends, BorderLayout.NORTH);
        side.add(new JScrollPane(contactList), BorderLayout.CENTER);
        side.add(southBtns, BorderLayout.SOUTH);

        // --- AREA CENTRAL (FEED/CHAT) ---
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(bgDark);
        
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(bgDark);

        JScrollPane scroll = new JScrollPane(messagesPanel);
        scroll.setBorder(null);
        center.add(scroll, BorderLayout.CENTER);

        // Input
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBackground(bgDark);
        bottom.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        inputField = new JTextField();
        inputField.setBackground(new Color(58, 59, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1, true), new EmptyBorder(8, 10, 8, 10)));
        
        JButton btnSend = new JButton("Publicar");
        styleButton(btnSend, accentBlue);
        
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        center.add(bottom, BorderLayout.SOUTH);

        // --- TOP BAR ---
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(bgSidebar);
        top.setPreferredSize(new Dimension(0, 60));
        top.setBorder(new MatteBorder(0, 0, 1, 0, Color.DARK_GRAY));

        logo = new JLabel("  ");
        logo.setForeground(accentBlue);
        logo.setFont(new Font("Arial", Font.BOLD, 22));
        
        styleButton(btnStatus, Color.GRAY);
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        topActions.setOpaque(false);
        topActions.add(btnStatus);

        top.add(logo, BorderLayout.WEST);
        top.add(topActions, BorderLayout.EAST);

        frame.add(side, BorderLayout.WEST);
        frame.add(center, BorderLayout.CENTER);
        frame.add(top, BorderLayout.NORTH);

        btnStatus.addActionListener(e -> toggleStatus());
        btnSend.addActionListener(e -> send());
        inputField.addActionListener(e -> send());
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void toggleStatus() {
        try {
            if (!isOnline) {
                server.login(myName, this);
                isOnline = true; 
                btnStatus.setText("Online");
                btnStatus.setBackground(new Color(49, 162, 76)); // Verde
            } else {
                server.logout(myName);
                isOnline = false; 
                btnStatus.setText("Entrar na Rede");
                btnStatus.setBackground(Color.GRAY);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void send() {
        String texto = inputField.getText().trim();
        String para = contactList.getSelectedValue();
        if(texto.isEmpty() || para == null) return;
        if(!isOnline) {
            JOptionPane.showMessageDialog(frame, "Conecte-se primeiro!");
            return;
        }
        try {
            server.enviarMensagem(myName, para, texto);
            addMessageToConversation(para, "Voce", texto);
            inputField.setText("");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refresh() throws RemoteException {
        List<String> lista = server.listarContatos(myName);
        contactModel.clear();
        for(String s : lista) contactModel.addElement(s);
        // garante histórico para cada contato
        for (String s : lista) conversations.computeIfAbsent(s, k -> new ArrayList<>());
        // atualiza a conversa exibida se houver seleção
        if (selectedContact != null) loadConversation(selectedContact);
    }


    private void loadConversation(String contact) {
        if (contact == null) {
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
            return;
        }
        List<String> msgs = conversations.getOrDefault(contact, new ArrayList<>());
        messagesPanel.removeAll();
        for (String m : msgs) {
            // mensagens salvas no formato "SENDER|HH:mm|texto"
            String[] parts = m.split("\\|", 3);
            String sender = parts.length > 0 ? parts[0] : "";
            String time = parts.length > 1 ? parts[1] : "";
            String text = parts.length > 2 ? parts[2] : "";
            boolean isOwn = sender.equalsIgnoreCase("VOCE") || sender.equalsIgnoreCase(myName.toUpperCase());
            addBubbleToPanel(sender, text, isOwn, time);
        }
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private void addMessageToConversation(String contact, String user, String msg) {
        List<String> list = conversations.computeIfAbsent(contact, k -> new ArrayList<>());
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        list.add(user.toUpperCase() + "|" + now + "|" + msg);
        if (contact.equals(selectedContact)) {
            boolean isOwn = user.equalsIgnoreCase(myName) || user.equalsIgnoreCase("Voce");
            addBubbleToPanel(user, msg, isOwn, now);
            messagesPanel.revalidate();
            messagesPanel.repaint();
        }
    }

    public void receberMensagem(String de, String texto) {
        SwingUtilities.invokeLater(() -> addMessageToConversation(de, de, texto));
    }

    private void addBubbleToPanel(String sender, String text, boolean isOwn, String time) {
        JPanel wrapper = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 4, 0, 4));

        Bubble bubble = new Bubble(text, isOwn ? new Color(45,136,255) : new Color(60,60,60), Color.WHITE, time);
        wrapper.add(bubble);

        messagesPanel.add(wrapper);
        // scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, messagesPanel);
            if (sp != null) {
                JViewport vp = sp.getViewport();
                vp.setViewPosition(new Point(0, messagesPanel.getHeight()));
            }
        });
    }

    // Painel com bolha arredondada
    private static class Bubble extends JPanel {
        private String text;
        private Color bg;
        private Color fg;
        private String time;

        Bubble(String text, Color bg, Color fg, String time) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            this.time = time;
            setOpaque(false);
            setLayout(new BorderLayout());
            JLabel lbl = new JLabel(formatHtml(text));
            lbl.setForeground(fg);
            lbl.setFont(new Font("Arial", Font.PLAIN, 14));
            lbl.setBorder(new EmptyBorder(6,12,6,12));
            add(lbl, BorderLayout.CENTER);

            JLabel timeLbl = new JLabel(time);
            timeLbl.setForeground(new Color(220,220,220));
            timeLbl.setFont(new Font("Arial", Font.PLAIN, 11));
            timeLbl.setBorder(new EmptyBorder(0,6,2,6));
            JPanel timePanel = new JPanel(new BorderLayout());
            timePanel.setOpaque(false);
            timePanel.add(timeLbl, BorderLayout.EAST);
            add(timePanel, BorderLayout.SOUTH);
        }

        private static String formatHtml(String t) {
            String safe = t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>");
            return "<html><body style='width:220px;'>" + safe + "</body></html>";
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 16;
            int w = getWidth();
            int h = getHeight();
            g2.setColor(bg);
            g2.fillRoundRect(0,0,w,h,arc,arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void addContactAction() {
        try {
            // Busca a lista atualizada de todos os usuários do servidor
            List<String> todosUsuarios = server.obterUsuariosGlobais();
            // Remove a si mesmo da lista para não seguir a si próprio
            todosUsuarios.remove(myName);

            if (todosUsuarios.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Ainda não há outros usuários registrados no sistema.");
                return;
            }

            // Transforma a lista em um array para o JOptionPane
            Object[] opcoes = todosUsuarios.toArray();
            String selecao = (String) JOptionPane.showInputDialog(
                    frame, 
                    "Escolha um perfil para seguir:", 
                    "SocialLink - Descobrir Pessoas", 
                    JOptionPane.PLAIN_MESSAGE, 
                    null, 
                    opcoes, 
                    opcoes[0]);

            if (selecao != null) {
                server.adicionarContato(myName, selecao);
                refresh();
                JOptionPane.showMessageDialog(frame, "Voce agora segue " + selecao);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(frame, "Erro ao buscar usuários: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            SwingChatClient c = new SwingChatClient();
            c.myName = JOptionPane.showInputDialog(null, "Nome do seu Perfil:", "SocialLink Login", JOptionPane.PLAIN_MESSAGE);
            if(c.myName == null) System.exit(0);
            c.server = (IChatServer) Naming.lookup("rmi://localhost:1099/ChatServer");
            // Registrar usuário (cria fila no servidor de mensagens)
            try { c.server.registrarUsuario(c.myName); } catch (Exception ex) { /* ignore if already exists */ }
            // Atualiza o header com o nome do usuário logado
            c.logo.setText("  " + c.myName);
            c.frame.setVisible(true);
            c.refresh();
        } catch (Exception e) { e.printStackTrace(); }
    }
}