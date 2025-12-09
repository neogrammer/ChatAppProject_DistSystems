using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _3420_Chat_Service.Models
{
    public class Message
    {
        [Key]
        public int Id { get; set; }
        
        [Required]
        public string UserId { get; set; } = string.Empty;
        
        [Required]
        public string UserName { get; set; } = string.Empty;
        
        [Required]
        public int GroupId { get; set; }
        
        [ForeignKey(nameof(GroupId))]
        public Group Group { get; set; } = null!;
        
        [Required]
        public string Content { get; set; } = string.Empty;
        
        [Required]
        public DateTime SentAt { get; set; }
    }
}