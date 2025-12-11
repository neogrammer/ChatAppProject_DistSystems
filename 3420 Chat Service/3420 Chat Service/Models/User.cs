using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _3420_Chat_Service.Models
{
    [Table("users")]
    public class User
    {
        [Key]
        [Column("id")]
        public long Id { get; set; }

        [Required]
        [Column("email")]
        [MaxLength(255)]
        public string Email { get; set; } = string.Empty;

        [Required]
        [Column("display_name")]
        [MaxLength(100)]
        public string DisplayName { get; set; } = string.Empty;

        [Column("password_hash")]
        [MaxLength(100)]
        public string PasswordHash { get; set; } = string.Empty;

        [Column("refresh_hash")]
        [MaxLength(64)]
        public string? RefreshHash { get; set; }

        [Column("refresh_expires")]
        public DateTime? RefreshExpires { get; set; }

        [Column("created_at")]
        public DateTime CreatedAt { get; set; }
    }
}
