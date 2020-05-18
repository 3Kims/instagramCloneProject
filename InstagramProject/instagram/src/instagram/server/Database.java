package instagram.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import instagram.config.ServerInfo;
import instagram.exception.DuplicateUserIdException;
import instagram.exception.RecordNotFoundException;
import instagram.vo.Comment;
import instagram.vo.Hashtag;
import instagram.vo.PersonTag;
import instagram.vo.Post;
import instagram.vo.User;

public class Database implements DatabaseTemplate{
	
	public static int COMMENTIDNUM=6;
	public static int POSTIDNUM=12;

	public Database(String serverIp) throws ClassNotFoundException{
		Class.forName(ServerInfo.DRIVE_NAME);
		System.out.println("드라이버 로딩 성공....");
	}
	
	@Override
	public Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection(ServerInfo.URL, ServerInfo.USER, ServerInfo.PASS);
		System.out.println("Database Connection");
		return conn;
	}

	@Override
	public void closeAll(PreparedStatement ps, Connection conn) throws SQLException {
		if(ps!=null) ps.close();
		if(conn!=null) conn.close();		
	}

	@Override
	public void closeAll(ResultSet rs, PreparedStatement ps, Connection conn) throws SQLException {
		if(rs!=null) rs.close();
		closeAll(ps, conn);			
	}
	
	public boolean isExistUserId(String userId, Connection conn)throws SQLException { // user 존재 유무 확인
		String sql ="SELECT userId FROM user WHERE userId=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		
		ps.setString(1, userId);
		ResultSet rs = ps.executeQuery();
		return rs.next();
	}
	
	public boolean isExistPostId(String postId, Connection conn)throws SQLException { //post 존재 유무 확인
		String sql ="SELECT postId FROM post WHERE postId=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		
		ps.setString(1,postId);
		ResultSet rs = ps.executeQuery();
		return rs.next();
	}
	
	public boolean isExistCommentId(String commentId, Connection conn)throws SQLException { //post 존재 유무 확인
		String sql ="SELECT commentId FROM post WHERE commentId=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		
		ps.setString(1,commentId);
		ResultSet rs = ps.executeQuery();
		return rs.next();
	}
	
	public boolean isExistHashtagId(String hashtagId, Connection conn)throws SQLException { //hashtagId 존재 유무 확인
		String sql ="SELECT hashtagId FROM hashtag WHERE hashtagId=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		
		ps.setString(1,hashtagId);
		ResultSet rs = ps.executeQuery();
		return rs.next();
	}
	

	
	
	
	
	@Override
	public void addUser(User user) throws SQLException, DuplicateUserIdException {
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			conn =getConnect();
			
			if(!isExistUserId(user.getUserId(), conn)) { // userId가 없다...
				String query = "INSERT INTO user(userId, userName, password, email, gender) values(?,?,?,?,?)";
				ps = conn.prepareStatement(query);

				ps.setString(1, user.getUserId());
				ps.setString(2, user.getUserName());
				ps.setString(3, user.getPassword());
				ps.setString(4, user.getEmail()); 
				ps.setString(5, user.getGender());
				
				int row =ps.executeUpdate();
				System.out.println(row +"명 추가 완료");
			}else { // 같은 userId 이미 존재
				throw new DuplicateUserIdException(user.getUserId()+"는 이미 존재합니다.");
			}
		}finally {
			closeAll(ps, conn);
		}
	}

	@Override
	public User getUser(String userId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		User user = null;
		
		try {
			conn =getConnect();

			if(isExistUserId(user.getUserId(), conn)) { 
				String query = "select userId, userName, followerNum, followingNum, postNum, email from user where userId = ?";
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
								
				rs = ps.executeQuery();
				if(rs.next()) {
					user = new User(rs.getString("userId"), rs.getString("userName"), rs.getInt("followerNum"), 
							rs.getInt("followingNum"), rs.getInt("postNum"), rs.getString("email"));
				}
			}else { // 같은 userId가 없다.
				throw new RecordNotFoundException(userId +"는 존재하지않습니다.");
			}
			}finally {
				closeAll(rs, ps, conn);
			}
		return user;
	}

	@Override
	public void updateUser(User user) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;		
		 try{
			 conn = getConnect();
			 
			 String query ="update user set userName =?, password=?, email =?, gender =? where userId=?";
			 ps = conn.prepareStatement(query);
			 ps.setString(1, user.getUserName());
			 ps.setString(2, user.getPassword());
			 ps.setString(3, user.getEmail());
			 ps.setString(4, user.getGender());
			 ps.setString(5, user.getUserId());
			
			 int row = ps.executeUpdate();
			 if(row==1) System.out.println(row+" 명 update success...");
			 else throw new RecordNotFoundException("수정할 대상이 없습니다.");
		 }finally{
			 closeAll(ps, conn);
		 }
	}

	@Override
	public void deleteUser(String userId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;	
		try{
			conn = getConnect();
			
			if(!isExistUserId(userId, conn)) { // userId가 없다...
			String query = "DELETE FROM user WHERE userId=?";			
			ps = conn.prepareStatement(query);
			ps.setString(1,userId);		
			
			ps.executeUpdate();
			System.out.println(userId+"가 삭제되었습니다");
			}else{
			throw new RecordNotFoundException(userId+ "가 존재하지않습니다.");
			}
		}finally{
			closeAll(ps, conn);			 
		}
	}

	@Override
	public ArrayList<User> getFollowerUsers(String userId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<User> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistUserId(userId, conn)) {
				String query = " select f.followingId, u.followerNum, u.followingNum, u.postNum, "
						+ "u.email from user u left join follow f on u.userId = f.userId "
						+ "where u.userId=? ";
				
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new User(rs.getString("followingId"), rs.getInt("followerNum"), 
							rs.getInt("followingNum"), rs.getInt("postNum"), rs.getString("email")));
				}
			}else {
				throw new RecordNotFoundException(userId+ "가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}

	@Override
	public ArrayList<User> getFollowingUsers(String userId) throws SQLException, RecordNotFoundException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<User> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistUserId(userId, conn)) {
				String query = " select u.userId, u.followerNum, u.followingNum, u.postNum, "
						+ "u.email from user u left join follow f on u.userId = f.userId "
						+ "where f.followingId=? ";
				
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new User(rs.getString("userId"), rs.getInt("followerNum"), 
							rs.getInt("followingNum"), rs.getInt("postNum"), rs.getString("email")));
				}
			}else {
				throw new RecordNotFoundException(userId+ "가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}

	@Override
	public void addComment(String userId, String postId, String comment) throws SQLException, RecordNotFoundException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		String commentId = "comm"+ COMMENTIDNUM;
		COMMENTIDNUM++;
		try{
			conn = getConnect();
			
			if(isExistUserId(userId, conn)){
				String query = "insert into comment(commentId, comment, userId, postId) values(?, ?, ?, ?)";			
				ps = conn.prepareStatement(query);
				ps.setString(1, commentId);
				ps.setString(2, comment);
				ps.setString(3, userId);
				ps.setString(4, postId);
				
				ps.executeUpdate();
				System.out.println(postId+"에" + userId +"님의 댓글이 달렸습니다.");
				}else{
				throw new RecordNotFoundException(userId+ "가 존재하지않습니다.");
				}
		}finally{
			closeAll(ps, conn);			 
		}
	}

	@Override
	public void updateComment(String userId, String postId, String commentId, String comment) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;	
		
		try{
			conn = getConnect();
			
			if(isExistCommentId(commentId, conn)){

				String query = "update comment set comment = ? where commentId = ? and postId =?";			
				ps = conn.prepareStatement(query);
				ps.setString(1, comment);
				ps.setString(2, commentId);
				ps.setString(3, postId);
				
				ps.executeUpdate();
				System.out.println("댓글이 수정되었습니다.");
				}else{
				throw new RecordNotFoundException(commentId+ " 존재하지않습니다.");
				}
		}finally{
			closeAll(ps, conn);			 
		}
	}

	@Override
	public void deleteComment(String userId, String postId, String commentId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;	
		
		try{
			conn = getConnect();
			
			if(isExistCommentId(commentId, conn)){

				String query = "delete from comment where commentId=? ";			
				ps = conn.prepareStatement(query);
				ps.setString(1, commentId);
				
				ps.executeUpdate();
				System.out.println("댓글이 삭되었습니다.");
				}else{
				throw new RecordNotFoundException(commentId+ "가 존재하지않습니다.");
				}
		}finally{
			closeAll(ps, conn);			 
		}
	}

	@Override
	public ArrayList<Comment> getCommentsOnPost(String userId ,String postId) throws SQLException, RecordNotFoundException { 
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Comment> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistPostId(postId, conn)) {
				String query = "select commentId, comment, userId, postId from comment where postId=?";
				
				ps = conn.prepareStatement(query);
				ps.setString(1, postId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new Comment(rs.getString("commentId"), rs.getString("comment"), userId, postId));
				}
			}else {
				throw new RecordNotFoundException(postId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}
	
	@Override
	public ArrayList<Comment> getCommentsUserWrite(String userId, String postId) throws SQLException, RecordNotFoundException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Comment> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistPostId(postId, conn)) {
				String query = "select commentId, comment, userId, postId from comment where userId=? and postId=?";
				
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				ps.setString(2, postId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new Comment(rs.getString("commentId"), rs.getString("comment"), userId, postId));
				}
			}else {
				throw new RecordNotFoundException(postId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}


	@Override
	public void addPost(String userId, Post post,String loginUserId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		
		try {
			conn = getConnect();
			conn.setAutoCommit(false); //시작
			String postId = "post"+ POSTIDNUM;
			String query =  "insert into post(postId, caption, imageSrc, ) values(?,?,?,)";
			ps = conn.prepareStatement(query);
			ps.setString(1, postId);
			ps.setString(2, post.getCaption());
			ps.setString(3, post.getImageSrc());
			ps.executeUpdate();
			
			String query1 =  "insert into persontag(userId, postId,postOwner) values(?, ?,'N')";
			ps1 = conn.prepareStatement(query1);
			ps.setString(1, userId);
			ps.setString(2, postId);
			ps.executeUpdate();
			
			String query3 =  "insert into persontag(userId, postId,postOwner) values(?, ?,'Y')";
			ps3 = conn.prepareStatement(query3);
			ps.setString(1, loginUserId);
			ps.setString(2, postId);
			ps.executeUpdate();
			
			String query2 = "update user set postNum = (select count(userId) from persontag where userId= ? and postOwner= 'Y') where userId=?";
			ps2 = conn.prepareStatement(query2);
			ps2.setString(1, loginUserId);
			ps2.setString(2, loginUserId);
			ps2.executeUpdate();
			
			System.out.println("게시물 업로드");
			conn.commit();

		} finally {
			closeAll(ps, conn);
			conn.setAutoCommit(true);
		} 
		
	}

	@Override
	public Post getPost(String userId, String postId) throws SQLException, RecordNotFoundException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Post post = null;
		
		try {
			conn = getConnect();
			if(isExistPostId(postId, conn)) {
				String query = "select * from post where postId =?";
				ps = conn.prepareStatement(query);
				ps.setString(1, postId);
				
				rs =ps.executeQuery();
				if(rs.next()) {
					new Post(rs.getString("postId"), rs.getString("caption"), rs.getString("imageSrc"), rs.getInt("likeNum"), rs.getString( "date"));
				}
			}else {
				throw new RecordNotFoundException(postId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return post;
	}

	@Override
	public ArrayList<Post> getAllPostsOfPerson(String userId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Post> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			
			if(isExistUserId(userId, conn)) {
				
				String query = "select po.postId, po.caption, po.imageSrc, po.likeNum, po.date, pe.userId "
						+ "from post po left join persontag pe "
						+ "on po.postId = pe.postId "
						+ "where pe.userId = ? and pe.postOwner ='Y'";
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				rs =ps.executeQuery();
				while(rs.next()) {
					
					list.add(new Post(rs.getString("po.postId"), rs.getString("po.caption"), rs.getString("po.imageSrc"), rs.getInt("po.likeNum") , rs.getString("po.date"), rs.getString("pe.userId")));
				}
			}else {
				throw new RecordNotFoundException(userId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}

	@Override
	public ArrayList<Post> getSomePostsOfFollowingPerson(String userId) throws SQLException, RecordNotFoundException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs=null;
		ArrayList<Post> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			
			if(isExistUserId(userId, conn)) {
				String query = "select p.postId, p.caption, p.imageSrc, p.likeNum, p.date, pt.userId from post p left join personTag pt on p.postId = pt.postId "
						+ "where p.postId in (select postId from persontag pt left join follow f on pt.userId = f.followingId where f.userId =? and pt.postOwner= 'Y') "
						+ "and pt.postOwner= 'Y'";
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new Post(rs.getString("p.postId"), rs.getString("p.caption"), rs.getString("p.imageSrc"), rs.getInt("p.likeNum"), rs.getString("p.date"), rs.getString("pt.userId")));
				}
			}else {
				throw new RecordNotFoundException(userId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}

	@Override
	public void updatePost(String userId, Post post) throws SQLException {
		
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			conn = getConnect();
			String query =  "UPDATE post SET caption =?, imageSrc=? where postId =?";
			ps = conn.prepareStatement(query);
			ps.setString(1, post.getCaption());
			ps.setString(2, post.getImageSrc());
			ps.setString(3, post.getPostId());
			
			ps.executeUpdate();
			System.out.println("게시글이 수정되었습니다.");
			
		} finally {
			closeAll(ps, conn);
		} 
		
	}

	@Override
	public void deletePost(String userId, String postId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		
		
		try {
			conn = getConnect();
			String query =  "delete from post where postId =?";
			ps = conn.prepareStatement(query);
			ps.setString(1, postId);	
			
			ps.executeUpdate();
			
		} finally {
			closeAll(ps, conn);
		} 
	}

	@Override
	public ArrayList<User> getUsersByPersonTag(String postId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<User> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistPostId(postId, conn)) {
				String query = "select userId, followerNum, followingNum, postNum, email from user where userId in (select userId from persontag where postId = ? and postOwner ='N')";
				ps = conn.prepareStatement(query);
				ps.setString(1, postId);
				
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new User(rs.getString("userId"), rs.getInt("followerNum"), rs.getInt("followingNum"),rs.getInt("postNum"), rs.getString("email")));
				}
			}else {
				throw new RecordNotFoundException(postId+"가 존재하지않습니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}

	@Override
	public ArrayList<Post> getPostsByHashTag(String hashtagId) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Post> list = new ArrayList<>();
		
		try {
			conn = getConnect();
			if(isExistHashtagId(hashtagId, conn)) {
				String query = "select p.postId, p.caption, p.imageSrc, p.likeNum, p.date, pt.userId from post p left join persontag pt on p.postId = pt.postId where p.postId in (select p3.postId from hashgroup p3 where p3.hashtagId = ?);";
				ps = conn.prepareStatement(query);
				ps.setString(1, hashtagId);
				rs =ps.executeQuery();
				while(rs.next()) {
					list.add(new Post(rs.getString("p.postId"), rs.getString("p.caption"), rs.getString("p.imageSrc"), rs.getInt("p.likeNum"),rs.getString("p.date"), rs.getString("pt.userId")));
				}
			}else {
				throw new RecordNotFoundException(hashtagId+"가 존재하지않습니다.");
			}
		}finally {
			closeAll(rs, ps, conn);
		}
		System.out.println(list+"데이타베이스-------------------");
		return list;
	}

	@Override
	public void authenticateUser(String userId, String password) throws SQLException, RecordNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			conn = getConnect();
			if(isExistUserId(userId, conn)) {
				String query = "select userId, password from user where userId =? and password=?";
				ps = conn.prepareStatement(query);
				ps.setString(1, userId);
				ps.setString(2, password);
				rs =ps.executeQuery();
				if(!rs.next()) {
					throw new RecordNotFoundException();
				}
			}else {
				throw new RecordNotFoundException(userId+"가 존재하지않거나 틀렸습니다..");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
	}

	public void checkUserId(String userId) throws DuplicateUserIdException, SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean result = false;
		try {
			conn = getConnect();
			if(!isExistUserId(userId, conn)) {
				result= true;
			}else {
				throw new DuplicateUserIdException("이미 존재합니다.");
			}
		
		}finally {
			closeAll(rs, ps, conn);
		}
	}
	
	public void likePost(String postId) throws SQLException { //트랜잭션 사용
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;
		int likeNum =0;
		
		try {
			conn = getConnect();
			conn.setAutoCommit(false); //시작
			System.out.println(postId);
			String query1 =  "select likeNum from post where postId=?";
			ps = conn.prepareStatement(query1);
			ps.setString(1, postId);	
			rs = ps.executeQuery();
			if(rs.next()) {
				 likeNum = rs.getInt("likeNum");
				}	
			String query2 =  "update post set likeNum = ?+1 where postId= ?";
			ps2 = conn.prepareStatement(query2);
			ps2.setInt(1, likeNum);
			ps2.setString(2, postId);

			
			ps2.executeUpdate();
			
			conn.commit();
			
		}catch(Exception e){
			
		}finally {
			closeAll(rs, ps, conn);
			conn.setAutoCommit(true);
		} 
	}
	


}
