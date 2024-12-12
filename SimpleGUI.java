import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import org.json.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SimpleGUI extends JFrame {
    private JButton queryButton;
    private JTextField ipInput;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton settingsButton;
    private static String currentToken = loadToken(); // 修改为从文件加载token
    private final ExecutorService executorService = Executors.newFixedThreadPool(3); // 使用3个线程的线程池
    private JProgressBar progressBar; // 添加进度条
    private JButton exportButton;
    private static final String TOKEN_FILE = "config.token";
    
    public SimpleGUI() {
        // 设置窗口标题
        super("IP查询工具");
        
        // 设置窗口大小
        setSize(800, 400);
        
        // 设置窗口关闭操作
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // 创建输入面板
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        
        // 创建输入组件
        ipInput = new JTextField(15);
        queryButton = new JButton("查询IP");
        settingsButton = new JButton("设置");
        exportButton = new JButton("导出结果");
        exportButton.setEnabled(false);
        
        inputPanel.add(new JLabel("IP地址："));
        inputPanel.add(ipInput);
        inputPanel.add(queryButton);
        inputPanel.add(settingsButton);
        inputPanel.add(exportButton);
        
        // 添加进度条到输入面板
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        inputPanel.add(progressBar);
        
        // 添加设置按钮点击事件
        settingsButton.addActionListener(e -> showSettingsDialog());
        
        // 添加导出按钮事件
        exportButton.addActionListener(e -> exportResults());
        
        // 创建表格
        String[] columnNames = {"IP地址", "归属地", "运营商", "邮政编码", "网络类型"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(30);
        resultTable.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 设置表格单元格渲染器
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            private final Color alternateColor = new Color(245, 245, 245); // 灰色行的颜色
            
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // 设置交替行的背景色
                if (row % 2 == 0) {
                    setBackground(Color.WHITE);
                } else {
                    setBackground(alternateColor);
                }
                
                setFont(new Font("微软雅黑", Font.PLAIN, 12));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
                ));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        
        // 设置表格头部样式
        JTableHeader header = resultTable.getTableHeader();
        header.setFont(new Font("微软雅黑", Font.BOLD, 12));
        header.setBackground(new Color(230, 230, 230));
        header.setForeground(Color.BLACK);
        
        // 添加自定义的表头渲染器
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);  // 设置居中对齐
                setBackground(new Color(230, 230, 230));
                setFont(new Font("微软雅黑", Font.BOLD, 12));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 0, 5, 0)
                ));
                return c;
            }
        };
        
        // 应用表头渲染器到所有列
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        
        // 应用单元格渲染器到所有列
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // 禁止表格编辑
        resultTable.setEnabled(false);
        
        // 创建带边框的滚动面板
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        // 添加按钮点击事件
        queryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String ips = ipInput.getText().trim();
                if (ips.isEmpty()) {
                    showError("请输入IP地址");
                    return;
                }
                
                // 清空现有数据
                while (tableModel.getRowCount() > 0) {
                    tableModel.removeRow(0);
                }
                
                // 分割IP地址
                String[] ipList = ips.split("[,，\\s]+");
                
                // 设置进度条
                progressBar.setMaximum(ipList.length);
                progressBar.setValue(0);
                progressBar.setVisible(true);
                queryButton.setEnabled(false);
                
                // 使用CompletableFuture进行并发查询
                CompletableFuture.runAsync(() -> {
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    AtomicInteger completedCount = new AtomicInteger(0);
                    
                    for (String ip : ipList) {
                        ip = ip.trim();
                        if (!ip.isEmpty()) {
                            String finalIp = ip;
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                try {
                                    String result = QueryHelper.queryIP(finalIp, currentToken);
                                    if (result != null) {
                                        addResultRow(result);
                                    } else {
                                        addErrorRow(finalIp, "查询失败");
                                    }
                                } catch (Exception ex) {
                                    addErrorRow(finalIp, ex.getMessage());
                                } finally {
                                    SwingUtilities.invokeLater(() -> {
                                        progressBar.setValue(completedCount.incrementAndGet());
                                    });
                                }
                            }, executorService);
                            futures.add(future);
                        }
                    }
                    
                    // 等待所有查询完成
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    // 恢复UI状态
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(false);
                        queryButton.setEnabled(true);
                        exportButton.setEnabled(tableModel.getRowCount() > 0);
                    });
                }, executorService);
            }
        });
        
        // 将组件添加到主面板
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 设置面板边距
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 将主面板添加到窗口
        add(mainPanel);
        
        // 设置窗口居中显示
        setLocationRelativeTo(null);
        
        // 设置列宽
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            TableColumn column = resultTable.getColumnModel().getColumn(i);
            switch (i) {
                case 0: // IP地址列
                    column.setPreferredWidth(120);
                    break;
                case 1: // 归属地列
                    column.setPreferredWidth(250);
                    break;
                case 2: // 运营商列
                    column.setPreferredWidth(150);
                    break;
                case 3: // 邮政编码列
                    column.setPreferredWidth(100);
                    break;
                case 4: // 网络类型列
                    column.setPreferredWidth(100);
                    break;
            }
        }
    }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            while (tableModel.getRowCount() > 0) {
                tableModel.removeRow(0);
            }
            Object[] rowData = new Object[5];
            rowData[0] = "错误";
            rowData[1] = message;
            tableModel.addRow(rowData);
        });
    }
    
    private void addResultRow(String jsonStr) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonStr);
            if (json.has("ret") && json.getString("ret").equals("ok")) {
                org.json.JSONArray dataArray = json.getJSONArray("data");
                if (dataArray.length() > 0) {
                    SwingUtilities.invokeLater(() -> {
                        Object[] rowData = new Object[5];
                        
                        // IP地址
                        rowData[0] = json.getString("ip");
                        
                        // 归属地
                        StringBuilder location = new StringBuilder();
                        for (int i = 0; i < Math.min(4, dataArray.length()); i++) {
                            String value = dataArray.getString(i);
                            if (!value.isEmpty() && !value.equals("null")) {
                                if (location.length() > 0) {
                                    location.append(" ");
                                }
                                location.append(value);
                            }
                        }
                        rowData[1] = location.toString();
                        
                        // 运营商
                        if (dataArray.length() > 4) {
                            rowData[2] = dataArray.getString(4);
                        }
                        
                        // 邮政编码
                        if (dataArray.length() > 5) {
                            rowData[3] = dataArray.getString(5);
                        }
                        
                        // 网络类型
                        if (dataArray.length() > 7) {
                            rowData[4] = dataArray.getString(7);
                        }
                        
                        tableModel.addRow(rowData);
                    });
                }
            } else {
                addErrorRow(json.getString("ip"), json.optString("msg", "未知错误"));
            }
        } catch (Exception e) {
            addErrorRow("未知", "解析结果失败：" + e.getMessage());
        }
    }
    
    private void addErrorRow(String ip, String error) {
        SwingUtilities.invokeLater(() -> {
            Object[] rowData = new Object[5];
            rowData[0] = ip;
            rowData[1] = "错误: " + error;
            tableModel.addRow(rowData);
        });
    }
    
    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, "设置", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Token标签和输入框
        JLabel tokenLabel = new JLabel("IP138 Token:");
        JTextField tokenField = new JTextField(currentToken);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        mainPanel.add(tokenLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(tokenField, gbc);
        
        // 状态标签
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.BLACK);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(statusLabel, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        JButton testButton = new JButton("测试Token");
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");
        
        // 初始状态下禁用保存按钮
        saveButton.setEnabled(false);
        
        // 测试按钮事件
        testButton.addActionListener(e -> {
            String testToken = tokenField.getText().trim();
            if (testToken.isEmpty()) {
                statusLabel.setText("Token不能为空");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            testButton.setEnabled(false);
            saveButton.setEnabled(false);
            statusLabel.setText("正在测试...");
            statusLabel.setForeground(Color.BLACK);
            
            new Thread(() -> {
                try {
                    String result = QueryHelper.queryIP("8.8.8.8", testToken);
                    if (result != null && result.contains("\"ret\":\"ok\"")) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Token有效");
                            statusLabel.setForeground(new Color(0, 150, 0));
                            saveButton.setEnabled(true);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Token无效");
                            statusLabel.setForeground(Color.RED);
                            saveButton.setEnabled(false);
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("测试失败: " + ex.getMessage());
                        statusLabel.setForeground(Color.RED);
                        saveButton.setEnabled(false);
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> testButton.setEnabled(true));
                }
            }).start();
        });
        
        // 保存按钮事件
        saveButton.addActionListener(e -> {
            String newToken = tokenField.getText().trim();
            if (!newToken.isEmpty()) {
                currentToken = newToken;
                saveToken(newToken); // 保存token到文件
                dialog.dispose();
            }
        });
        
        // 取消按钮事件
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // 添加组件到对话框
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置对话框属性
        dialog.setSize(450, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // 显示对话框
        dialog.setVisible(true);
    }
    
    // QueryHelper 内部类
    private static class QueryHelper {
        private static String DATATYPE = "json";
        private static final int CONNECT_TIMEOUT = 3000; // 3秒连接超时
        private static final int READ_TIMEOUT = 3000;    // 3秒读取超时
        
        public static String get(String urlString, String token) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("token", token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Connection", "close"); // 不使用持久连接
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        return br.lines().collect(Collectors.joining("\n"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }
        
        public static String queryIP(String ip, String token) {
            String url = "https://api.ip138.com/ipdata/?ip=" + ip + "&datatype=" + DATATYPE;
            return get(url, token);
        }
    }
    
    // 在类的最后添加关闭线程池的方法
    @Override
    public void dispose() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        super.dispose();
    }
    
    // 添加导出方法
    private void exportResults() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "没有可导出的数据", "导出", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // 生成带时间戳的文件名
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "IP查询结果_" + timestamp + ".csv";
            
            // 创建CSV文件
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
                // 写入BOM，解决Excel打开中文乱码问题
                writer.write('\ufeff');
                
                // 写入表头
                StringBuilder header = new StringBuilder();
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (i > 0) header.append(",");
                    header.append(escapeCSV(tableModel.getColumnName(i)));
                }
                writer.write(header.toString());
                writer.newLine();
                
                // 写入数据
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    StringBuilder line = new StringBuilder();
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        if (col > 0) line.append(",");
                        Object value = tableModel.getValueAt(row, col);
                        line.append(escapeCSV(value != null ? value.toString() : ""));
                    }
                    writer.write(line.toString());
                    writer.newLine();
                }
                
                JOptionPane.showMessageDialog(this, 
                    "导出成功：" + fileName, 
                    "导出完成", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "导出失败：" + ex.getMessage(),
                "导出错误",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 添加CSV转义方法
    private String escapeCSV(String value) {
        if (value == null) return "";
        
        // 如果包含逗号、引号或换行符，需要用引号包围
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // 将引号替换为两个引号
            value = value.replace("\"", "\"\"");
            // 用引号包围整个值
            return "\"" + value + "\"";
        }
        return value;
    }
    
    // 添加加载token的方法
    private static String loadToken() {
        try {
            File file = new File(TOKEN_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    return reader.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // 如果没有保存的token，返回空字符串
    }
    
    // 添加保存token的方法
    private void saveToken(String token) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
            writer.write(token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // 在事件调度线程中运行GUI
        SwingUtilities.invokeLater(() -> {
            new SimpleGUI().setVisible(true);
        });
    }
}