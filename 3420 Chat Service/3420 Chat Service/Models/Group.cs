using System.ComponentModel.DataAnnotations;

namespace _3420_Chat_Service.Models
{
    public class Group
    {
        [Key]
        public Guid Id { get; set; }
        
        [Required]
        public string GroupName { get; set; } = string.Empty;
        
        public ICollection<Message> Messages { get; set; } = new List<Message>();
        public ICollection<GroupMember> GroupMembers { get; set; } = new List<GroupMember>();
    }
}